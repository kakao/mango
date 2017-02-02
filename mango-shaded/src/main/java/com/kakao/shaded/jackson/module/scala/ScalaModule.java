package com.kakao.shaded.jackson.module.scala;

import com.kakao.shaded.jackson.module.scala.DefaultScalaModule;

/**
 * @deprecated Use {@link DefaultScalaModule}
 */
@Deprecated
public class ScalaModule extends DefaultScalaModule
{
    private static final String NAME = "ScalaModule";
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public ScalaModule() { }

    @Override public String getModuleName() { return NAME; }
}
