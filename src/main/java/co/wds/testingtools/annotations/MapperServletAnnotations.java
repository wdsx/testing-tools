package co.wds.testingtools.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


import org.eclipse.jetty.servlet.ServletHolder;

import co.wds.testingtools.annotations.mapperservlet.AnnotationMapperServlet;
import co.wds.testingtools.annotations.mapperservlet.TestingServer;

public class MapperServletAnnotations {
	
	private static TestingServer server;
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface TestServlet {
		int port() default 80;
		String contentType() default "application/json";
		boolean requiresAuthentication() default false;
		String userName() default "";
		String password() default "";
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface RespondTo {
		ResponseData[] value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface ResponseData {
		String url() default "/";
		String resourceFile();
		String contentType() default "";
		int status() default 200;
	}

	public static void startMapperServlet(Object testObject) {
		try {
			processAnnotations(Class.forName(testObject.getClass().getName()), testObject);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void stopMapperServlet() {
		try {
			if (server != null) {
				server.stop();
				server = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void processAnnotations(final Class<? extends Object> testClass,	Object testObject) throws Exception {
		TestServlet testServlet = testClass.getAnnotation(TestServlet.class);
		createServer(testServlet);
		
		RespondTo respondTo = testClass.getAnnotation(RespondTo.class);
		addResponsesFrom(respondTo, testServlet);
		
		startServer();
	}

	private static void createServer(TestServlet testServlet) {
		if (testServlet != null) {
			server = new TestingServer(testServlet.port());
		}
	}

	private static void addResponsesFrom(RespondTo respondTo, TestServlet testServlet) {
		if (server != null && respondTo != null) {
			AnnotationMapperServlet ms = new AnnotationMapperServlet();
			ms.setRequiresAuthentication(testServlet.requiresAuthentication());
			ms.setAuthenticationAllowedUserName(testServlet.userName());
			ms.setAuthenticationAllowedPassword(testServlet.password());
			for (ResponseData response : respondTo.value()) {
				String contentType = response.contentType();
				if ("".equals(contentType)) {
					contentType = testServlet.contentType();
				}
				String uri = response.url();
				if (!uri.startsWith("/")) {
					uri = "/" + uri;
				}
				String resourceFile = response.resourceFile();
				int status = response.status();
				
				ms.bindReponse(uri, resourceFile, contentType, status);
			}
			
			ServletHolder holder = new ServletHolder(ms);
			server.getHandler().addServlet(holder, "/");
		}
	}

	private static void startServer() throws Exception {
		if (server != null) {
			server.start();
		}
	}
}