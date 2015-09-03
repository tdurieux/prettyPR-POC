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

        // Tells the Jersey Servlet which REST service/class to load.
        jerseyServlet.setInitParameter("jersey.config.server.provider.classnames",
                                              EntryPoint.class.getCanonicalName());

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
