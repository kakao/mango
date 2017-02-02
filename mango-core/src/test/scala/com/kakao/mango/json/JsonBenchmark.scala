package com.kakao.mango.json

import com.kakao.shaded.jackson.core.JsonToken

object JsonBenchmark extends App {

  case class ImpressionInfo(impressionId: String)
  case class Impression(info: ImpressionInfo)

  def data: Array[Byte] = {
    """{"info":{"requestId":"talk-channel","impressionId":"talknews:d58c0cff70384619ba467bc6e037ce98:14e431291cd"},"result":{"count":15,"channelId":"talknews","userId":"2524338","userProfile":{"p":{"birth":"1993","gender":"F"},"f":{},"s":{"d-1-6":1.0},"us":"d-1-6"},"bucket":{"id":63,"name":"smp:p1:smp-cc-random:cache:","algo":"smp","profile":"p1","matrix":"","pctr":"smp-cc-random","cache":true},"items":[{"id":"68633","title":"고용형태공시 후폭풍 \"대기업이 나쁜 일자리 양산\"","header":"","link":"http://m.media.daum.net/m/channel/view/media/20150630144218651","subTitle":"","subLink":"","ctime":"20150630145553","features":{},"pctrs":{"smp-cc-random":0.014084507042253521},"category":{"L4":-1,"L3":-1,"L2":1006,"L1":1000},"isBreakingNews":true,"position":1},{"id":"68630","title":"일본 신칸센 열차 안에서 승객 분신 2명 사망","header":"","link":"http://m.media.daum.net/m/channel/view/media/20150630142908985","subTitle":"","subLink":"","ctime":"20150630144254","features":{},"pctrs":{"smp-cc-random":0.015873015873015872},"category":{"L4":-1,"L3":-1,"L2":1007,"L1":1000},"isBreakingNews":false,"position":100},{"id":"68588","title":"북한 단속정 서해 NLL 침범..경고사격 받고 퇴각","header":"","link":"http://m.media.daum.net/m/channel/view/media/20150630140017719","subTitle":"","subLink":"","ctime":"20150630141716","features":{},"pctrs":{"smp-cc-random":0.010691823899371069},"category":{"L4":-1,"L3":-1,"L2":1002,"L1":1000},"isBreakingNews":false,"position":100},{"id":"68591","title":"신경숙, 잘못 만져 자꾸 덧나는 상처","header":"","link":"http://m.media.daum.net/m/channel/view/media/20150630140009703","subTitle":"","subLink":"","ctime":"20150630141900","features":{},"pctrs":{"smp-cc-random":0.010582010582010581},"category":{"L4":-1,"L3":-1,"L2":1003,"L1":1000},"isBreakingNews":false,"position":100},{"id":"68329","title":"카카오톡 샵(#) 검색, 모바일 검색 판도 바꿀까","header":"","link":"http://m.media.daum.net/m/channel/view/media/20150630111513236","subTitle":"","subLink":"","ctime":"20150630113554","features":{},"pctrs":{"smp-cc-random":0.01026167265264238},"category":{"L4":-1,"L3":-1,"L2":1008,"L1":1000},"isBreakingNews":false,"position":100},{"id":"68335","title":"맹독 문어 피해자 \"의사도 처음엔 웃어..엄청난 통증\"","header":"","link":"http://m.media.daum.net/m/channel/view/media/20150630113030028","subTitle":"","subLink":"","ctime":"20150630114743","features":{},"pctrs":{"smp-cc-random":0.009768637532133676},"category":{"L4":-1,"L3":-1,"L2":1001,"L1":1000},"isBreakingNews":false,"position":100},{"id":"68597","title":"라이베리아서 최근 사망 10대 시신,  에볼라 양성 반응","header":"","link":"http://m.media.daum.net/m/channel/view/media/20150630135320474","subTitle":"","subLink":"","ctime":"20150630141900","features":{},"pctrs":{"smp-cc-random":0.008875739644970414},"category":{"L4":-1,"L3":-1,"L2":1007,"L1":1000},"isBreakingNews":false,"position":100},{"id":"68617","title":"대법 \"돈 빌릴 때 갚을 능력 있었다면 사기죄 안돼\"","header":"","link":"http://m.media.daum.net/m/channel/view/media/20150630140921188","subTitle":"","subLink":"","ctime":"20150630143507","features":{},"pctrs":{"smp-cc-random":0.008733624454148471},"category":{"L4":-1,"L3":-1,"L2":1001,"L1":1000},"isBreakingNews":false,"position":100},{"id":"68309","title":"스펙만 쌓는 요즘 대학생? 누군 하고 싶어 하나요","header":"","link":"http://m.media.daum.net/m/channel/view/media/20150630105207789","subTitle":"","subLink":"","ctime":"20150630110426","features":{},"pctrs":{"smp-cc-random":0.008},"category":{"L4":-1,"L3":-1,"L2":1006,"L1":1000},"isBreakingNews":false,"position":100},{"id":"68571","title":"일 유명 관광지 하코네산 분화..경계수준 3으로 높여","header":"","link":"http://m.media.daum.net/m/channel/view/media/20150630134025959","subTitle":"","subLink":"","ctime":"20150630140148","features":{},"pctrs":{"smp-cc-random":0.006578947368421052},"category":{"L4":-1,"L3":-1,"L2":1007,"L1":1000},"isBreakingNews":false,"position":100},{"id":"68506","title":"남편대신 돈벌어 오는 아내 급증..작년 5만가구","header":"","link":"http://m.media.daum.net/m/channel/view/media/20150630130814892","subTitle":"","subLink":"","ctime":"20150630131838","features":{},"pctrs":{"smp-cc-random":0.006109979633401222},"category":{"L4":-1,"L3":-1,"L2":1006,"L1":1000},"isBreakingNews":false,"position":100},{"id":"68311","title":"'일가족 참변 음주 살인운전' 화물차 기사 구속","header":"","link":"http://m.media.daum.net/m/channel/view/media/20150630105211797","subTitle":"","subLink":"","ctime":"20150630110543","features":{},"pctrs":{"smp-cc-random":0.005707297187117815},"category":{"L4":-1,"L3":-1,"L2":1001,"L1":1000},"isBreakingNews":false,"position":100},{"id":"68613","title":"4대강 보 물새고 떨어져 나가고..2년간 보수 216건","header":"","link":"http://m.media.daum.net/m/channel/view/media/20150630141809499","subTitle":"","subLink":"","ctime":"20150630143507","features":{},"pctrs":{"smp-cc-random":0.005319148936170213},"category":{"L4":-1,"L3":-1,"L2":1001,"L1":1000},"isBreakingNews":false,"position":100},{"id":"68592","title":"\"악의냐, 선의냐\" 검찰수사로 드러난 '고발왕' 진면목","header":"","link":"http://m.media.daum.net/m/channel/view/media/20150630135915691","subTitle":"","subLink":"","ctime":"20150630141900","features":{},"pctrs":{"smp-cc-random":0.004987531172069825},"category":{"L4":-1,"L3":-1,"L2":1001,"L1":1000},"isBreakingNews":false,"position":100},{"id":"68584","title":"'한국판 바지소송' 수선했더니 수백만원 내놔라?","header":"","link":"http://m.media.daum.net/m/channel/view/media/20150630134611175","subTitle":"","subLink":"","ctime":"20150630140956","features":{},"pctrs":{"smp-cc-random":0.0045045045045045045},"category":{"L4":-1,"L3":-1,"L2":1001,"L1":1000},"isBreakingNews":false,"position":100}]}}""".getBytes("UTF-8")
  }

  def stream(): String = {
    streamJson(data).collectFirst {
      case (JsonToken.VALUE_STRING, StringField("impressionId", value)) => value
    }.get
  }

  def from(): String = {
    fromJson[Impression](data).info.impressionId
  }

  def parse(): String = {
    parseJson(data)("info").asInstanceOf[Map[String,Any]]("impressionId").toString
  }

}
