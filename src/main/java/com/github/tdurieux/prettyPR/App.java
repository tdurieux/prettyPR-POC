package com.github.tdurieux.prettyPR;

import com.github.tdurieux.prettyPR.api.v0.EntryPoint;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class App {

    public static void main(String[] args) throws Exception {
        System.setProperty("org.eclipse.jetty.LEVEL", "INFO");

        Server server = new Server(8080);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setResourceBase(App.class.getResource("/www").getPath());
        context.setContextPath("/");
        server.setHandler(context);

        ServletHolder jerseyServlet = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/api/v.0/*");
        jerseyServlet.setInitOrder(0);

        jerseyServlet.setInitParameter("com.sun.jersey.config.property.packages",
                EntryPoint.class.getCanonicalName());
        jerseyServlet.setInitParameter("jersey.config.server.provider.classnames",
                                              EntryPoint.class.getCanonicalName());
        jerseyServlet.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");


        context.addServlet(new ServletHolder(new DefaultServlet()), "/*");

        try {
            server.start();
            server.join();
        } finally {
            server.stop();
            server.destroy();
        }
    }
}
