package gitcc;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;

import com.ibm.rational.clearcase.remote_core.rpc.proxy.IProxySetting;
import com.ibm.rational.clearcase.remote_core.rpc.proxy.IProxySettingProvider;
import com.ibm.rational.clearcase.remote_core.rpc.proxy.ProxySettingProviderRegistry;

public class ProxyHelper {

	private static Logger log = Logger.getLogger(ProxyHelper.class);
	
	public static void initProxy() {
		String httpProxy = System.getenv("http_proxy");
		if (httpProxy == null)
			return;
		try {
			setProxy(new URL(httpProxy));
		} catch (MalformedURLException e) {
			log.error(e);
		}
	}

	public static void setProxy(final URL url) {
		ProxySettingProviderRegistry.register(new IProxySettingProvider() {
			@Override
			public IProxySetting getProxySetting() {
				return new IProxySetting() {
					@Override
					public String getHostname() {
						return url.getHost();
					}

					@Override
					public int getPort() {
						return url.getPort();
					}
				};
			}
		});
	}
}
