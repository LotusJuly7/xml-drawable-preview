package com.example.xmldrawablepreview;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class PreviewServer {
    public interface Listener { void onXmlReceived(byte[] xml); }

    private final int port;
    private final Listener listener;
    private volatile boolean running;
    private ServerSocket socket;
    private Thread thread;

    public PreviewServer(int port, Listener listener) {
        this.port = port;
        this.listener = listener;
    }

    public void start() throws IOException {
        socket = new ServerSocket(port);
        running = true;
        thread = new Thread(new Runnable() {
            public void run() { acceptLoop(); }
        }, "xml-preview-http");
        thread.start();
    }

    public void stop() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) { }
    }

    private void acceptLoop() {
        while (running) {
            try {
                final Socket client = socket.accept();
                new Thread(new Runnable() {
                    public void run() { handle(client); }
                }, "xml-preview-client").start();
            } catch (IOException e) {
                if (running) e.printStackTrace();
            }
        }
    }

    private void handle(Socket client) {
        try {
            HttpParams params = new BasicHttpParams();
            BasicHttpProcessor processor = new BasicHttpProcessor();
            processor.addInterceptor(new ResponseDate());
            processor.addInterceptor(new ResponseServer());
            processor.addInterceptor(new ResponseContent());
            processor.addInterceptor(new ResponseConnControl());
            HttpRequestHandlerRegistry registry = new HttpRequestHandlerRegistry();
            registry.register("/preview", new PreviewHandler());
            HttpService service = new HttpService(processor, null, null, params);
            service.setHandlerResolver(registry);
            DefaultHttpServerConnection connection = new DefaultHttpServerConnection();
            connection.bind(client, params);
            HttpContext context = new BasicHttpContext(null);
            while (running && connection.isOpen()) {
                service.handleRequest(connection, context);
            }
        } catch (Exception ignored) {
        } finally {
            try { client.close(); } catch (IOException ignored) { }
        }
    }

    private class PreviewHandler implements HttpRequestHandler {
        public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
            if (!"POST".equalsIgnoreCase(request.getRequestLine().getMethod())) {
                response.setStatusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
                response.setEntity(new StringEntity("POST raw XML bytes to /preview\n"));
                return;
            }
            if (!(request instanceof org.apache.http.HttpEntityEnclosingRequest)) {
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                response.setEntity(new StringEntity("Missing request body\n"));
                return;
            }
            HttpEntity entity = ((org.apache.http.HttpEntityEnclosingRequest) request).getEntity();
            byte[] bytes = EntityUtils.toByteArray(entity);
            listener.onXmlReceived(bytes);
            response.setStatusCode(HttpStatus.SC_OK);
            response.setEntity(new StringEntity("OK " + bytes.length + " bytes\n"));
        }
    }
}
