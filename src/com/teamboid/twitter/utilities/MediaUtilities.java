package com.teamboid.twitter.utilities;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.FlickrApi;
import org.scribe.builder.api.ImgUrApi;
import org.scribe.oauth.OAuthService;

public class MediaUtilities {
	
	public static OAuthService buildAuthService(String service){
		return buildAuthService(service, "null://null.jpg");
	}
	
	public static OAuthService buildAuthService(String service, String callback){
		if(service.equals("imgur")){
			return new ServiceBuilder()
							.provider(ImgUrApi.class)
							.apiKey("ee7a6c73396f12b50ea7890b05fc6e4404fd5ffa2")
							.apiSecret("2f0470bdae5e2238a112c2edd317c5cf")
							.callback(callback)
							.build();
		} else if(service.equals("flickr")){
			return new ServiceBuilder()
							.provider(FlickrApi.class)
							.apiKey("7085d1fbd99658bb8c3b20c4dbdd43e9")
							.apiSecret("fca3b92a15cb8bbd")
							.callback(callback)
							.build();
		} else{
			return null;
		}
	}
}
