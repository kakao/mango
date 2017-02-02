package com.kakao.mango.zk

import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.atomic.AtomicBoolean

import com.kakao.mango.logging.Logging
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.curator.framework.api.{BackgroundCallback, CuratorEvent}
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.BoundedExponentialBackoffRetry
import org.apache.zookeeper.Watcher.Event.EventType
import org.apache.zookeeper.data.Stat
import org.apache.zookeeper.{CreateMode, KeeperException, WatchedEvent, Watcher}
import org.apache.zookeeper.KeeperException.Code
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

/** provides methods for creating ZooKeeper connections
  */
object ZooKeeperConnection extends Logging {

  /** create a [[ZooKeeperConnection]] using information in Typesafe config */
  def apply(conf: Config = ConfigFactory.load().getConfig("zookeeper")): ZooKeeperConnection = {
    val connectString = Try(conf.getString("connectString")).get
    val sessionTimeout = Try(conf.getInt("sessionTimeout")).get
    val connectionTimeout = Try(conf.getInt("connectionTimeout")).get

    new ZooKeeperConnection(connectString, sessionTimeout, connectionTimeout)
  }

  /** create a [[ZooKeeperConnection]] with given connect string and 6000 ms session timeout */
  def apply(connectString: String): ZooKeeperConnection = apply(connectString, 6000)

  /** create a [[ZooKeeperConnection]] with given connect string and session timeout */
  def apply(connectString: String, sessionTimeout: Int): ZooKeeperConnection = apply(connectString, sessionTimeout, sessionTimeout)

  /** create a [[ZooKeeperConnection]] with given connect string and session/connection timeout */
  def apply(connectString: String, sessionTimeout: Int, connectionTimeout: Int): ZooKeeperConnection = {
    new ZooKeeperConnection(connectString, sessionTimeout, connectionTimeout)
  }

  private[zk] def callback(function: (Code, CuratorEvent) => Unit): BackgroundCallback = {
    new BackgroundCallback {
      override def processResult(client: CuratorFramework, event: CuratorEvent): Unit = {
        function(Code.get(event.getResultCode), event)
      }
    }
  }

  private[zk] def watcher(subscribed: AtomicBoolean)(function: (EventType, Watcher) => Unit): Watcher = {
    new Watcher {
      override def process(event: WatchedEvent): Unit = {
        if (subscribed.get()) {
          function(event.getType, this)
        }
      }
    }
  }
}

/** A class representing a connection to a ZooKeeper cluster, wrapping Apache Courator */
class ZooKeeperConnection(val connectString: String, val sessionTimeout: Int, val connectionTimeout: Int) {
  // lazy connection
  lazy val retryPolicy = new BoundedExponentialBackoffRetry(1000, 32000, 24)
  lazy val client = {
    val client = CuratorFrameworkFactory.newClient(connectString, sessionTimeout, connectionTimeout, retryPolicy)
    client.start()
    client
  }

  def close() = client.close()

  /** returns an instance of [[ZNode]], that provides access to ZooKeeper node data */
  def apply(path: String*) = new ZNode(client, "/" + path.mkString("/").stripPrefix("/"))
}

/** data structure for ZooKeeper node data and metadata */
case class ZNodeData(path: String, data: Array[Byte], stat: Stat) {
  lazy val string = new String(data, UTF_8)
}

/** A handle for a ZooKeeper node at a specific path, where the node may not necessarily exist.
  * Many asynchronous actions on the ZooKeeper node, including creating one, can be performed.
  *
  * @param path   the path of the ZooKeeper node.
  */
class ZNode(val client: CuratorFramework, val path: String) {
  import scala.concurrent.ExecutionContext.Implicits.global
  import org.apache.zookeeper.CreateMode._
  import org.apache.zookeeper.KeeperException.Code._
  import org.apache.zookeeper.Watcher.Event.EventType._
  import scala.collection.JavaConversions._
  import ZooKeeperConnection.{callback, watcher, logger}

  /** return a children node of this node, with given name */
  def child(name: String) = new ZNode(client, path + (if (path.endsWith("/")) "" else "/") + name)

  /** return the data in this node, as a UTF-8 String */
  def string(): Future[String] = get().map(_.string)

  /** return the data in this node, as a byte array */
  def bytes(): Future[Array[Byte]] = get().map(_.data)

  /** return the Stat of this node, None if this node does not exist */
  def exists(): Future[Option[Stat]] = {
    val promise = Promise[Option[Stat]]()
    client.checkExists().inBackground(callback {
      case (OK, event) => promise.success(Option(event.getStat))
      case (code, _) => promise.failure(KeeperException.create(code, path))
    }).forPath(path)
    promise.future
  }

  /** return the data and stat of this node, as a [[ZNodeData]] instance */
  def get(): Future[ZNodeData] = {
    val promise = Promise[ZNodeData]()
    client.getData.inBackground(callback {
      case (OK, event) => promise.success(ZNodeData(path, event.getData, event.getStat))
      case (code, _) => promise.failure(KeeperException.create(code, path))
    }).forPath(path)
    promise.future
  }

  /** Set the data in this node as given String */
  def set(data: String): Future[Unit] = set(data.getBytes(UTF_8))

  /** set the data in this node as given byte array */
  def set(data: Array[Byte]): Future[Unit] = {
    val promise = Promise[Unit]()
    client.setData().inBackground(callback {
      case (OK, _) => promise.success(())
      case (code, _) => promise.failure(KeeperException.create(code, path))
    }).forPath(path, data)
    promise.future
  }

  /** Set the data in this node as given String, creating the node if it does not exist */
  def setOrCreate(data: String, mode: CreateMode = PERSISTENT): Future[Unit] = setOrCreate(data.getBytes(UTF_8), mode)

  /** Set the data in this node as given byte array, creating the node if it does not exist */
  def setOrCreate(data: Array[Byte], mode: CreateMode): Future[Unit] = {
    set(data).recoverWith({
      case _: KeeperException.NoNodeException => createOrSet(data, mode)
    })
  }

  /** Create a node at the path, with given String as its data */
  def create(data: String, mode: CreateMode = PERSISTENT): Future[Unit] = create(data.getBytes(UTF_8), mode)

  /** Create a node at the path, with given byte array as its data */
  def create(data: Array[Byte], mode: CreateMode): Future[Unit] = {
    val promise = Promise[Unit]()
    client.create().creatingParentsIfNeeded.withMode(mode).inBackground(callback {
      case (OK, _) => promise.success(())
      case (code, _) => promise.failure(KeeperException.create(code, path))
    }).forPath(path, data)
    promise.future
  }

  /** Create a node at the path, with given String as its data, only setting the value if the node exists */
  def createOrSet(data: String, mode: CreateMode = PERSISTENT): Future[Unit] = createOrSet(data.getBytes(UTF_8), mode)

  /** Create a node at the path, with given byte array as its data, only setting the value it the node exists */
  def createOrSet(data: Array[Byte], mode: CreateMode): Future[Unit] = {
    create(data, mode).recoverWith({
      case _: KeeperException.NodeExistsException => setOrCreate(data, mode)
    })
  }

  /** Return the names of the children nodes of this node */
  def children(): Future[Seq[String]] = {
    val promise = Promise[Seq[String]]()
    client.getChildren.inBackground(callback {
      case (OK, event) => promise.success(event.getChildren)
      case (code, _) => promise.failure(KeeperException.create(code))
    }).forPath(path)
    promise.future
  }

  /** Return the names of the children nodes, or an empty sequence even if this node does not exist */
  def childrenOrEmpty(): Future[Seq[String]] = {
    children().recover({
      case _: KeeperException.NoNodeException => Nil
    })
  }

  /** Delete this node */
  def delete(): Future[Unit] = {
    val promise = Promise[Unit]()
    client.delete().inBackground(callback {
      case (OK, _) => promise.success(())
      case (code, _) => promise.failure(KeeperException.create(code, path))
    }).forPath(path)
    promise.future
  }

  /** Delete this node, ignoring exception if it does not exist */
  def deleteOrIgnore(): Future[Unit] = {
    delete().recover({
      case _: KeeperException.NoNodeException => ()
    })
  }

  /** Delete this node and all its children recursively */
  def deleteAll(): Future[Unit] = {
    children().flatMap(Future.traverse(_)(child(_).deleteAll())).flatMap(_ => delete())
  }

  /** Delete this node and all its children recursively, ignoring exception if it does not exist */
  def deleteAllOrIgnore(): Future[Unit] = {
    childrenOrEmpty().flatMap(Future.traverse(_)(child(_).deleteAllOrIgnore())).flatMap(_ => deleteOrIgnore())
  }

  /** Watch any data changes on this node, and calls the callback method
    *
    * @param listener   the callback method to be called when the data changes
    * @return           A [[Subscription]] instance that provides a way to unsubscribe further notifications.
    */
  def watch(listener: Try[String] => Unit): Subscription = {
    watch().subscribe (
      value => listener(Success(value)),
      error => listener(Failure(error))
    )
  }

  /** Return an Rx [[Observable]] instance that watches any data changes on this node */
  def watch(): Observable[String] = watchBytes().map(new String(_, UTF_8))

  /** Watch any data changes on this node, and calls the callback method
    *
    * @param listener   the callback method to be called when the data changes
    * @return           A [[Subscription]] instance that can unsubscribe further notifications.
    */
  def watchBytes(listener: Try[Array[Byte]] => Unit): Subscription = {
    watchBytes().subscribe (
      value => listener(Success(value)),
      error => listener(Failure(error))
    )
  }

  /** Return an Rx [[Observable]] instance that watches any data changes on this node */
  def watchBytes(): Observable[Array[Byte]] = {
    Observable.create { observer =>
      val subscribed = new AtomicBoolean(true)
      val watch = (watcher: Watcher) =>
        client.getData.usingWatcher(watcher).inBackground(callback {
          case (OK, event) => observer.onNext(event.getData)
          case (code, _) => observer.onError(KeeperException.create(code, path))
        }).forPath(path)
      watch(watcher(subscribed) {
        case (NodeDataChanged, watcher) => watch(watcher)
        case (NodeDeleted, _) => observer.onError(KeeperException.create(NONODE, path))
        case (None, _) => // ignore connection state change notifications
        case (event, _) => logger.error(s"Unexpected watched event type $event for path $path")
      })
      Subscription {
        subscribed.set(false)
      }
    }
  }

  /** Watch any children changes on this node, and calls the callback method
    *
    * @param listener   the callback method to be called when the children list changes
    * @return           A [[Subscription]] instance that can unsubscribe further notifications.
    */
  def watchChildren(listener: Try[Seq[String]] => Unit): Subscription = {
    watchChildren().subscribe (
      value => listener(Success(value)),
      error => listener(Failure(error))
    )
  }

  /** Return an Rx [[Observable]] instance that watches any children changes on this node */
  def watchChildren(): Observable[Seq[String]] = {
    Observable.create { observer =>
      val subscribed = new AtomicBoolean(true)
      val watch = (watcher: Watcher) =>
        client.getChildren.usingWatcher(watcher).inBackground(callback {
          case (OK, event) => observer.onNext(event.getChildren)
          case (code, _) => observer.onError(KeeperException.create(code, path))
        }).forPath(path)
      watch(watcher(subscribed) {
        case (NodeChildrenChanged, watcher) => watch(watcher)
        case (NodeDeleted, _) => observer.onError(KeeperException.create(NONODE, path))
        case (None, _) => // ignore connection state change notifications
        case (event, _) => logger.error(s"Unexpected watched event type $event for path $path")
      })
      Subscription {
        subscribed.set(false)
      }
    }
  }

}