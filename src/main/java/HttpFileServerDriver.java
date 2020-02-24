import httpClient.Httpc;
import com.beust.jcommander.JCommander;
import http.Constants;
import httpFileServer.Httpfs;
import logger.Logger;

import java.util.Map;

/**
 * Starting class of the HTTP Library project
 *
 * @author Kerry Gougeon Ducharme (40028722) and Jonathan Mongeau (40006501)
 */
class HttpFileServerDriver {
    public static void main(String[] args) {
        Httpfs httpfs = new Httpfs();

        JCommander jc = httpfs.getJc();
        if (args.length == 0) {
            jc.usage();
            return;
        }

        httpfs.interpret(args).ifPresent(response -> {
            Logger.debug(
                    response.getVersion() + Constants.SPACE + response.getStatus() + Constants.SPACE + response.getPhrase());
            for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
                Logger.debug(header.getKey() + ": " + header.getValue());
            }
            Logger.println(response.getBody());
        });


    }
}
