package server;

import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutionException;

public class JSCompressor {

    public static String getCompressedJS(String js) throws ExecutionException, InterruptedException {
        String body = getBody(js);
        var client = HttpClient.newBuilder().build();

        var req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofMinutes(3))
                .uri(URI.create("https://closure-compiler.appspot.com/compile"))
                .build();
        var json = client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(res ->  new JSONObject(res.body()))
                .get();

        System.out.println("json = " + json);
        System.out.println("json.getString(\"errors\") = " + json.get("errors"));

        System.out.println(json.getJSONArray("errors").getJSONObject(0).getString("line").substring(2900, 3000));
        return json.getString("compiledCode");
    }

    private static String getBody(String js){
        return String.format("js_code=%s" +
                "&compilation_level=SIMPLE_OPTIMIZATIONS" +
                "&output_format=json&" +
                "output_info=compiled_code" +
                        "&output_info=errors" +
                        "&output_info=statistics" +
                        "&output_info=warnings" +
                        "&language=ECMAScript6",
                URLEncoder.encode(js, StandardCharsets.UTF_8)) ;
    }
}
