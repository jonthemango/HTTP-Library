package http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.Iterator;
import java.util.StringJoiner;


/**
 * Request object which can represent any
 * http request. Specified by it's method, url, headers and body.
 */
public class Request {

    private Method method;
    private String version = "HTTP/1.0";

    private InetAddress address;
    private String path;
    private Headers headers;
    private String body;

    public static final Integer MAXTRIES = 5; // Max tries for redirect as specified in HTTP 1.0 Redirection specification

    public Request(URL url, Method method, Headers headers, String body){
        this.setURL(url);
        this.method = method;
        this.body = body;
        this.headers = headers;
    }

    public Request(URL url, Method method, Headers headers, String body, String version) {
        this.setURL(url);
        this.method = method;
        this.body = body;
        this.headers = headers;
        this.version = version;
    }

    public Method getMethod() {
        return method;
    }

    public String getVersion() {
        return version;
    }

    public InetAddress getAddress() {
        return address;
    }

    public String getPath() {
        return path;
    }

    public Headers getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    /**
     * Private setter for setting the URI of the location the lib
     * is trying to access
     * @param url
     */
    private void setURL(URL url){
        this.path = url.getPath();
        if(url.getQuery() != null && !url.getQuery().isEmpty()){
            this.path  += "?" + url.getQuery();
        }
        try {
            this.address = InetAddress.getByName(url.getHost());
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("No such URL is known");
        }
    }

    /**
     * Private getter to generate the first line in the http request.
     * @return requestLine
     */
    private StringBuilder getRequestLine(){
        StringBuilder sb = new StringBuilder();
        sb.append(method)
          .append(Constants.SPACE)
          .append(this.path)
          .append(Constants.SPACE)
          .append(this.version)
          .append(Constants.CARRIAGE)
          .append(Constants.NEW_LINE);

        return sb;
    }

    /**
     * Private getter to generate the the serialized request
     * @return serializedRequest
     */
    private String getSerialized(){
        StringBuilder sb = getRequestLine();
        sb.append(headers)
          .append(Constants.CARRIAGE)
          .append(Constants.NEW_LINE)
          .append(this.body);

        return sb.toString();
    }

    /**
     * This method sends a single request and returns a single response (ie. this method does not support redirection)
     * @return A single response object
     */
    private Response sendIsolatedRequest() {
        Socket socket = null;
        Response response = null;
        try {

            socket = new Socket(this.address, 80);

            OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
            BufferedReader in = new BufferedReader (new InputStreamReader(socket.getInputStream()));

            if(this.method.equals(Method.POST) && this.body != null && !this.body.isEmpty()){
                this.headers.put("Content-Length", String.valueOf(this.body.getBytes().length));
            }

            String serialized = this.getSerialized();
            out.write(serialized);
            out.flush();

            response = Response.fromBufferedReader(in);

            out.close();
            in.close();
            socket.close();

        } catch (IOException e) {
            throw new IllegalArgumentException("Can't establish an connection.");
        }
        return response;
    }

    /**
     * This public send method
     * It supports redirection for Responses who's status code
     * is in the 300 range.
     * @return Resulting response object after MAXTRIES redirections
     */
    public Response send(){

        Response response = this.sendIsolatedRequest();
        String location;
        Headers headers;
        URL url;

        Integer noTries = 0;
        while (response.getStatus() % 300 <= 99 && noTries < this.MAXTRIES){
            headers = response.getHeaders();
            location = headers.get("Location");
            try {
                url = new URL(location);
            } catch (MalformedURLException e) {
                break;
            }

            this.setURL(url);

            response = this.sendIsolatedRequest();
            noTries++;
        }
        return response;
    }


    public static Request fromBufferedReader(BufferedReader in) throws IOException {
        RequestBuilder rb = new RequestBuilder();
        Headers headers = new Headers();
        boolean firstLine = true;
        boolean iteratorReachedBody = false;
        boolean done = false;
        StringBuilder body = new StringBuilder();
        Method method = null;

        Iterator<String> iterator = in.lines().iterator();
        while(!done) {
            String line = iterator.next();
            System.out.println(line);
            if (firstLine) {
                firstLine = false;
                final String[] split = line.split(" ");
                method = Method.valueOf(split[0]);
                rb.setMethod(method)
                  .setUrl(new URL( "http://www.foo.com" + split[1]))
                  .setVersion(split[2]);
            } else if( line.isEmpty() ) {
                if(iteratorReachedBody || method == Method.GET) done = true;
                iteratorReachedBody = true;
            }  else if(iteratorReachedBody){
                body.append(line).append(Constants.NEW_LINE); // Adding a /n so it's matches what we originally receive
            } else {
                String[] split = line.split(":");
                headers.put(split[0], line.replace(split[0], "").replace(": ", ""));
            }
        }

        return rb.setHeaders(headers)
                 .setBody(body.toString())
                 .createRequest();
        }


    @Override public String toString() {
        return new StringJoiner(", ", Request.class.getSimpleName() + "[", "]").add("method=" + method)
                                                                               .add("version='" + version + "'")
                                                                               .add("address=" + address)
                                                                               .add("path='" + path + "'")
                                                                               .add("headers=" + headers)
                                                                               .add("body='" + body + "'")
                                                                               .toString();
    }
}
