package dev.horoz.url_shortener.service;

import dev.horoz.url_shortener.domain.Link;
import dev.horoz.url_shortener.domain.User;
import dev.horoz.url_shortener.dto.link.LinkResponseDto;
import dev.horoz.url_shortener.exceptions.SlugAlreadyExistsException;
import dev.horoz.url_shortener.mapper.LinkMapper;
import dev.horoz.url_shortener.repository.LinkRepository;
import dev.horoz.url_shortener.repository.UserRepository;
import dev.horoz.url_shortener.service.slug.SlugGenerator;
import dev.horoz.url_shortener.service.validation.UrlValidationService;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class LinkServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    LinkRepository linkRepository;
    @Mock
    SlugGenerator slugGenerator;
    @Mock
    UrlValidationService urlValidationService;
    @Mock
    Authentication authentication;

    LinkService service;

    @BeforeEach
    void setUp() {
        service = new LinkService(userRepository, linkRepository, slugGenerator, urlValidationService);
    }

    // ----------------------------------------------------------------------
    // createLink()
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("createLink: если ссылка уже есть у пользователя -> возвращает существующую, не сохраняет новую")
    void createLink_whenExistingLinkPresent_returnsExisting_andDoesNotSave() {
        String email = "a@b.com";
        String rawUrl = " https://example.com ";
        String validUrl = "https://example.com";
        User user = givenUser(email);

        Link existing = givenLink(user, validUrl, "EXIST123");

        when(authentication.getName()).thenReturn(email);
        when(urlValidationService.normalizeAndValidateUrl(rawUrl)).thenReturn(validUrl);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(linkRepository.findByUserAndTargetUrlIgnoreCase(user, validUrl)).thenReturn(Optional.of(existing));

        LinkResponseDto dto = service.createLink(authentication, rawUrl, null);

        assertThat(dto).isEqualTo(LinkMapper.toDto(existing));

        verify(linkRepository, never()).saveAndFlush(any(Link.class));
        verify(slugGenerator, never()).nextSlug();
    }

    @Test
    @DisplayName("createLink: customSlug null/blank -> идёт в генерацию slug и сохраняет")
    void createLink_whenCustomSlugBlank_generatesSlug_andSaves() {
        String email = "a@b.com";
        String rawUrl = "https://example.com";
        String validUrl = "https://example.com";
        User user = givenUser(email);

        when(authentication.getName()).thenReturn(email);
        when(urlValidationService.normalizeAndValidateUrl(rawUrl)).thenReturn(validUrl);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(linkRepository.findByUserAndTargetUrlIgnoreCase(user, validUrl)).thenReturn(Optional.empty());

        when(slugGenerator.nextSlug()).thenReturn("GEN12345");
        // saveAndFlush успешно
        when(linkRepository.saveAndFlush(any(Link.class))).thenAnswer(inv -> inv.getArgument(0));

        LinkResponseDto dto = service.createLink(authentication, rawUrl, "   ");

        assertThat(dto).isNotNull();
        verify(slugGenerator, times(1)).nextSlug();
        verify(linkRepository, times(1)).saveAndFlush(argThat(l ->
                validUrl.equals(l.getTargetUrl())
                        && "GEN12345".equals(l.getSlug())
                        && l.getUser() == user
                        && l.getExpiresAt() != null
        ));
    }

    @Test
    @DisplayName("createLink: customSlug задан -> сохраняет с customSlug")
    void createLink_whenCustomSlugProvided_savesWithCustomSlug() {
        String email = "a@b.com";
        String rawUrl = "https://example.com";
        String validUrl = "https://example.com";
        User user = givenUser(email);

        when(authentication.getName()).thenReturn(email);
        when(urlValidationService.normalizeAndValidateUrl(rawUrl)).thenReturn(validUrl);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(linkRepository.findByUserAndTargetUrlIgnoreCase(user, validUrl)).thenReturn(Optional.empty());

        when(linkRepository.saveAndFlush(any(Link.class))).thenAnswer(inv -> inv.getArgument(0));

        LinkResponseDto dto = service.createLink(authentication, rawUrl, "MYSLUG01");

        assertThat(dto).isNotNull();

        verify(slugGenerator, never()).nextSlug();
        verify(linkRepository, times(1)).saveAndFlush(argThat(l ->
                "MYSLUG01".equals(l.getSlug()) && validUrl.equals(l.getTargetUrl()) && l.getUser() == user
        ));
    }

    @Test
    @DisplayName("createLink: customSlug конфликтует по unique constraint -> SlugAlreadyExistsException")
    void createLink_whenCustomSlugNotUnique_throwsSlugAlreadyExists() {
        String email = "a@b.com";
        String rawUrl = "https://example.com";
        String validUrl = "https://example.com";
        User user = givenUser(email);

        when(authentication.getName()).thenReturn(email);
        when(urlValidationService.normalizeAndValidateUrl(rawUrl)).thenReturn(validUrl);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(linkRepository.findByUserAndTargetUrlIgnoreCase(user, validUrl)).thenReturn(Optional.empty());

        DataIntegrityViolationException uniqueViolation = uniqueSlugViolation();
        when(linkRepository.saveAndFlush(any(Link.class))).thenThrow(uniqueViolation);

        assertThatThrownBy(() -> service.createLink(authentication, rawUrl, "TAKEN123"))
                .isInstanceOf(SlugAlreadyExistsException.class);

        verify(linkRepository, times(1)).saveAndFlush(any(Link.class));
        verify(slugGenerator, never()).nextSlug();
    }

    @Test
    void createLink_whenGeneratedSlugConflicts_retriesAndSucceeds() {

        String email = "a@b.com";
        String rawUrl = "https://example.com";
        String validUrl = "https://example.com";

        User user = givenUser(email);

        when(authentication.getName()).thenReturn(email);
        when(urlValidationService.normalizeAndValidateUrl(rawUrl)).thenReturn(validUrl);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(linkRepository.findByUserAndTargetUrlIgnoreCase(user, validUrl))
                .thenReturn(Optional.empty());

        // 3 разных slug
        when(slugGenerator.nextSlug()).thenReturn("S1", "S2", "S3");

        DataIntegrityViolationException uniqueViolation =
                uniqueSlugViolation();

        List<String> slugsAtSave = new ArrayList<>();

        when(linkRepository.saveAndFlush(any(Link.class)))
                .thenAnswer(inv -> {
                    Link l = inv.getArgument(0);
                    slugsAtSave.add(l.getSlug());
                    throw uniqueViolation;
                })
                .thenAnswer(inv -> {
                    Link l = inv.getArgument(0);
                    slugsAtSave.add(l.getSlug());
                    throw uniqueViolation;
                })
                .thenAnswer(inv -> {
                    Link l = inv.getArgument(0);
                    slugsAtSave.add(l.getSlug());
                    return l;
                });

        LinkResponseDto dto = service.createLink(authentication, rawUrl, null);

        assertThat(dto).isNotNull();

        // проверяем количество попыток
        verify(slugGenerator, times(3)).nextSlug();
        verify(linkRepository, times(3)).saveAndFlush(any(Link.class));

        // проверяем, что реально пытались сохранить S1 → S2 → S3
        assertThat(slugsAtSave).containsExactly("S1", "S2", "S3");
    }


    @Test
    @DisplayName("createLink: generated slug конфликтует MAX_ATTEMPTS раз -> IllegalStateException")
    void createLink_whenGeneratedSlugConflictsAlways_throwsIllegalState() {
        String email = "a@b.com";
        String rawUrl = "https://example.com";
        String validUrl = "https://example.com";
        User user = givenUser(email);

        when(authentication.getName()).thenReturn(email);
        when(urlValidationService.normalizeAndValidateUrl(rawUrl)).thenReturn(validUrl);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(linkRepository.findByUserAndTargetUrlIgnoreCase(user, validUrl)).thenReturn(Optional.empty());

        // MAX_ATTEMPTS = 10
        when(slugGenerator.nextSlug()).thenReturn("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");
        when(linkRepository.saveAndFlush(any(Link.class))).thenThrow(uniqueSlugViolation());

        assertThatThrownBy(() -> service.createLink(authentication, rawUrl, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not generate unique slug after 10 attempts");

        verify(slugGenerator, times(10)).nextSlug();
        verify(linkRepository, times(10)).saveAndFlush(any(Link.class));
    }

    @Test
    @DisplayName("createLink: DataIntegrityViolationException НЕ про unique slug -> пробрасывается как есть")
    void createLink_whenDataIntegrityNotUniqueSlug_isRethrown() {
        String email = "a@b.com";
        String rawUrl = "https://example.com";
        String validUrl = "https://example.com";
        User user = givenUser(email);

        when(authentication.getName()).thenReturn(email);
        when(urlValidationService.normalizeAndValidateUrl(rawUrl)).thenReturn(validUrl);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(linkRepository.findByUserAndTargetUrlIgnoreCase(user, validUrl)).thenReturn(Optional.empty());

        when(slugGenerator.nextSlug()).thenReturn("GEN12345");
        DataIntegrityViolationException other = new DataIntegrityViolationException("boom");
        when(linkRepository.saveAndFlush(any(Link.class))).thenThrow(other);

        assertThatThrownBy(() -> service.createLink(authentication, rawUrl, null))
                .isSameAs(other);

        verify(slugGenerator, times(1)).nextSlug();
        verify(linkRepository, times(1)).saveAndFlush(any(Link.class));
    }

    // ----------------------------------------------------------------------
    // getLinks()
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("getLinks: вызывает findAllByUser с pageable (sort createdAt desc) и маппит в dto")
    void getLinks_callsRepositoryWithPageable() {
        String email = "a@b.com";
        User user = givenUser(email);

        when(authentication.getName()).thenReturn(email);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

        Link l1 = givenLink(user, "https://a.com", "S1");
        Page<Link> page = new PageImpl<>(List.of(l1));

        when(linkRepository.findAllByUser(eq(user), any(Pageable.class))).thenReturn(page);

        Page<LinkResponseDto> result = service.getLinks(authentication, 1, 20);

        assertThat(result.getContent()).hasSize(1);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(linkRepository).findAllByUser(eq(user), captor.capture());

        Pageable p = captor.getValue();
        assertThat(p.getPageNumber()).isEqualTo(1);
        assertThat(p.getPageSize()).isEqualTo(20);
        assertThat(Objects.requireNonNull(p.getSort().getOrderFor("createdAt")).getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    // ----------------------------------------------------------------------
    // getLinkById / stats / delete / update (ветки ownership + not found)
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("getLinkById: если link не найден у пользователя -> 404")
    void getLinkById_whenNotFound_returns404() {
        String email = "a@b.com";
        User user = givenUser(email);
        UUID id = UUID.randomUUID();

        when(authentication.getName()).thenReturn(email);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(linkRepository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLinkById(authentication, id))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("deleteLinkById: если link найден -> delete вызывается")
    void deleteLinkById_whenFound_deletes() {
        String email = "a@b.com";
        User user = givenUser(email);
        UUID id = UUID.randomUUID();
        Link link = givenLink(user, "https://x.com", "S1");
        setId(link, id);

        when(authentication.getName()).thenReturn(email);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(linkRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(link));

        service.deleteLinkById(authentication, id);

        verify(linkRepository).delete(link);
    }

    @Test
    @DisplayName("updateLink: если link найден -> expiresAt обновляется (становится > now)")
    void updateLink_whenFound_updatesExpiresAt() {
        String email = "a@b.com";
        User user = givenUser(email);
        UUID id = UUID.randomUUID();
        Link link = givenLink(user, "https://x.com", "S1");
        setId(link, id);


        Instant before = Instant.now();
        link.setExpiresAt(Instant.EPOCH);

        when(authentication.getName()).thenReturn(email);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(linkRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(link));

        LinkResponseDto dto = service.updateLink(authentication, id);

        assertThat(dto).isNotNull();
        assertThat(link.getExpiresAt()).isAfter(before);
    }

    // ----------------------------------------------------------------------
    // processRedirect()
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("processRedirect: slug не найден -> 404")
    void processRedirect_whenSlugNotFound_throws404() {
        when(linkRepository.findBySlug("abc")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processRedirect("abc"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(linkRepository, never()).incrementClicksTotal(any(UUID.class));
    }

    @Test
    @DisplayName("processRedirect: ссылка истекла -> 410 GONE, клики не инкрементятся")
    void processRedirect_whenExpired_throws410() {
        Link link = new Link();
        setId(link, UUID.randomUUID());
        link.setTargetUrl("https://target.com");
        link.setExpiresAt(Instant.now().minusSeconds(5));

        when(linkRepository.findBySlug("abc")).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> service.processRedirect("abc"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.GONE));

        verify(linkRepository, never()).incrementClicksTotal(any(UUID.class));
    }

    @Test
    @DisplayName("processRedirect: валидная ссылка -> incrementClicksTotal вызывается, возвращается targetUrl")
    void processRedirect_whenValid_incrementsAndReturnsTargetUrl() {
        Link link = new Link();
        UUID id = UUID.randomUUID();
        setId(link, id);
        link.setTargetUrl("https://target.com");
        link.setExpiresAt(Instant.now().plusSeconds(60));

        when(linkRepository.findBySlug("abc")).thenReturn(Optional.of(link));

        String target = service.processRedirect("abc");

        assertThat(target).isEqualTo("https://target.com");
        verify(linkRepository).incrementClicksTotal(id);
    }

    // ----------------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------------

    private User givenUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hash");

        setId(user, UUID.randomUUID());

        return user;
    }

    private Link givenLink(User user, String targetUrl, String slug) {
        Link link = new Link();
        link.setUser(user);
        link.setTargetUrl(targetUrl);
        link.setSlug(slug);
        link.setExpiresAt(Instant.now().plusSeconds(3600));

        setId(link, UUID.randomUUID());

        return link;
    }


    /**
     * Делает DataIntegrityViolationException с цепочкой причин,
     * где есть Hibernate ConstraintViolationException с constraintName.
     * Именно это ищет isUniqueSlugViolation().
     */
    private DataIntegrityViolationException uniqueSlugViolation() {
        ConstraintViolationException cve =
                new ConstraintViolationException("constraint", null, "links_slug_uidx");
        return new DataIntegrityViolationException("DUP", cve);
    }

    private static <T> void setId(T entity, UUID id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
