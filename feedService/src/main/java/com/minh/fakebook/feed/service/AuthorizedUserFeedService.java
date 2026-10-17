package com.minh.fakebook.feed.service;

import java.util.*;
import com.minh.fakebook.feed.repository.FeedItemRepository;
import com.minh.fakebook.feed.service.dto.FeedItemDTO;
import com.minh.fakebook.feed.service.mapper.FeedItemMapper;
import com.minh.fakebook.feed.client.FeedReadClient;
import com.minh.fakebook.feed.client.SocialContextClient;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Legacy offset endpoint: apply authoritative membership before the externally visible page. */
@Service
@Transactional(readOnly=true)
public class AuthorizedUserFeedService {
    private final FeedItemRepository items;
    private final FeedItemMapper mapper;
    private final FeedReadClient posts;
    private final SocialContextClient social;
    public AuthorizedUserFeedService(FeedItemRepository items,FeedItemMapper mapper,FeedReadClient posts,SocialContextClient social) {
        this.items=items;this.mapper=mapper;this.posts=posts;this.social=social;
    }
    public Page<FeedItemDTO> getUserFeed(UUID viewer,Pageable page) {
        var context=social.read(viewer);
        if(context==null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Social authorization unavailable");
        List<FeedItemDTO> visible=new ArrayList<>();
        for(int index=0;;index++) {
            var batch=items.findByUserIdOrderByCreatedAtDescPostIdDesc(viewer,PageRequest.of(index,100));
            if(batch.isEmpty()) break;
            var grants=posts.readable(new FeedReadClient.ReadRequest(batch.getContent().stream().map(p -> p.getPostId()).toList(),viewer,context.friends()));
            if(grants==null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Post authorization unavailable");
            Set<UUID> allowed=new HashSet<>();grants.forEach(p -> allowed.add(p.postId()));
            batch.getContent().stream().filter(p -> allowed.contains(p.getPostId())).map(mapper::toDto).forEach(visible::add);
            if(!batch.hasNext()) break;
        }
        int start=(int)Math.min(page.isPaged()?page.getOffset():0,visible.size());
        int end=page.isPaged()?Math.min(start+page.getPageSize(),visible.size()):visible.size();
        return new PageImpl<>(visible.subList(start,end),page,visible.size());
    }
}
