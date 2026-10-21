package com.minh.fakebook.media.service;

import java.io.*;
import java.net.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Relays a single byte range without buffering an asset in memory. */
@Service
public class MediaFileProxy {
    HttpURLConnection openConnection(URI uri) throws IOException { return (HttpURLConnection) uri.toURL().openConnection(); }

    public ResponseEntity<?> fetch(String url, String range) {
        URI uri = URI.create(url);
        if (!"https".equals(uri.getScheme()) || !"res.cloudinary.com".equals(uri.getHost()) || uri.getUserInfo() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid storage host");
        }
        if (range != null && !range.matches("bytes=(?:[0-9]+-[0-9]*|-[0-9]+)")) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
        }
        HttpURLConnection connection = null;
        try {
            connection = openConnection(uri);
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(5000); connection.setReadTimeout(30000);
            if (range != null) connection.setRequestProperty(HttpHeaders.RANGE, range);
            int status = connection.getResponseCode();
            HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl("private, no-store");
            headers.set("Referrer-Policy", "no-referrer");
            for (String name : new String[]{HttpHeaders.CONTENT_TYPE, HttpHeaders.CONTENT_LENGTH, HttpHeaders.CONTENT_RANGE, HttpHeaders.ACCEPT_RANGES}) {
                String value = connection.getHeaderField(name);
                if (value != null) headers.set(name, value);
            }
            if (status == 416) { connection.disconnect(); headers.remove(HttpHeaders.CONTENT_LENGTH); return ResponseEntity.status(416).headers(headers).build(); }
            if (status != 200 && status != 206) {
                connection.disconnect();
                throw new ResponseStatusException(status >= 500 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY, "Storage returned HTTP " + status);
            }
            HttpURLConnection upstream = connection;
            InputStream stream = new FilterInputStream(connection.getInputStream()) {
                @Override public void close() throws IOException { try { super.close(); } finally { upstream.disconnect(); } }
            };
            return ResponseEntity.status(status).headers(headers).body(new InputStreamResource(stream));
        } catch (IOException error) {
            if (connection != null) connection.disconnect();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Storage temporarily unavailable", error);
        }
    }
}
