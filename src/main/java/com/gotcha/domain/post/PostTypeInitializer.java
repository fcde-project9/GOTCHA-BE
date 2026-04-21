package com.gotcha.domain.post;

import com.gotcha.domain.post.entity.PostType;
import com.gotcha.domain.post.repository.PostTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostTypeInitializer implements ApplicationRunner {

    private final PostTypeRepository postTypeRepository;

    @Override
    public void run(ApplicationArguments args) {
        initPostType("갓챠일상", "일상 이야기를 나눠요");
        initPostType("궁금해요", "궁금한 것을 물어보세요");
        initPostType("거래해요", "거래 관련 게시글");
    }

    private void initPostType(String typeName, String description) {
        if (!postTypeRepository.existsByTypeName(typeName)) {
            postTypeRepository.save(PostType.builder()
                    .typeName(typeName)
                    .description(description)
                    .build());
            log.info("PostType initialized: {}", typeName);
        }
    }
}
