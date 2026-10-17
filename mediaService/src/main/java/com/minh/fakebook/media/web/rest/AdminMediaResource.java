package com.minh.fakebook.media.web.rest;

import java.net.URI;
import java.util.*;
import com.minh.fakebook.media.administration.AdminMediaService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/media")
@PreAuthorize("hasAuthority('ROLE_ADMIN') and !hasAuthority('ROLE_INTERNAL')")
public class AdminMediaResource {
    private final AdminMediaService media;
    public AdminMediaResource(AdminMediaService media) {this.media=media;}
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> file(@PathVariable UUID id) {
        String url=media.auditedLocation(id);
        byte[] bytes;MediaType type;
        if(url.startsWith("data:")) {
            String[] parts=url.split(",",2);
            type=MediaType.parseMediaType(parts[0].substring(5,parts[0].indexOf(';')));
            bytes=Base64.getDecoder().decode(parts[1]);
        } else {
            URI location=URI.create(url);
            if(!"https".equals(location.getScheme()) || !"res.cloudinary.com".equals(location.getHost())) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);
            var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();factory.setConnectTimeout(3000);factory.setReadTimeout(30000);
            var response=RestClient.builder().requestFactory(factory).build().get().uri(location).retrieve().toEntity(byte[].class);
            bytes=response.getBody();type=response.getHeaders().getContentType();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(type==null?MediaType.APPLICATION_OCTET_STREAM:type).body(bytes);
    }
}
