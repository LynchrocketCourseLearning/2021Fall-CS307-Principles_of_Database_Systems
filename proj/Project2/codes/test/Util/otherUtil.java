package test.Util;

import cn.edu.sustech.cs307.config.Config;
import cn.edu.sustech.cs307.factory.ServiceFactory;

public abstract class otherUtil {
    protected static final ServiceFactory SERVICE_FACTORY= Config.getServiceFactory();

    public otherUtil() {
        throw new RuntimeException("No Util Instance for you!");
    }
}