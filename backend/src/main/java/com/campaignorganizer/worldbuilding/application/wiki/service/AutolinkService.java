package com.campaignorganizer.worldbuilding.application.wiki.service;

import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ApplyAutolinksUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ArticleCommands.UpdateArticleCommand;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.AutolinkDtos.AutolinkCandidateGroup;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.AutolinkDtos.AutolinkMatch;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.AutolinkDtos.AutolinkSelection;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ScanAutolinkCandidatesUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.UpdateArticleUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleAliasRepositoryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleRepositoryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import com.campaignorganizer.worldbuilding.domain.wiki.Article;
import com.campaignorganizer.worldbuilding.domain.wiki.AutolinkScanner;
import com.campaignorganizer.worldbuilding.domain.wiki.AutolinkScanner.Candidate;
import com.campaignorganizer.worldbuilding.domain.wiki.AutolinkScanner.Occurrence;
import com.campaignorganizer.worldbuilding.domain.wiki.AutolinkScanner.Selection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Auto-link scan/apply use cases (ADR-0116) - article bodies only (not
 * beats/roll tables/card decks, which use a separate plain-textarea +
 * client-side wiki-link renderer, ADR-0115). Applying reuses the existing
 * {@link UpdateArticleUseCase} rather than writing a new persistence path,
 * so an auto-linked change gets the same revision-history snapshot
 * (ADR-0026) as any manual edit. */
@Service
public class AutolinkService implements ScanAutolinkCandidatesUseCase, ApplyAutolinksUseCase {

    private final ArticleRepositoryPort articles;
    private final ArticleAliasRepositoryPort aliases;
    private final WorldExistsPort worlds;
    private final UpdateArticleUseCase updateUseCase;

    public AutolinkService(ArticleRepositoryPort articles, ArticleAliasRepositoryPort aliases,
                           WorldExistsPort worlds, UpdateArticleUseCase updateUseCase) {
        this.articles = articles;
        this.aliases = aliases;
        this.worlds = worlds;
        this.updateUseCase = updateUseCase;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutolinkCandidateGroup> scan(UUID worldId) {
        requireWorld(worldId);
        List<Article> worldArticles = articles.findByWorld(worldId);
        List<Candidate> candidates = buildCandidates(worldId, worldArticles);

        List<AutolinkCandidateGroup> groups = new ArrayList<>();
        for (var article : worldArticles) {
            List<Occurrence> occurrences = AutolinkScanner.scan(article.getBody(), article.getId(), candidates);
            if (occurrences.isEmpty()) {
                continue;
            }
            List<AutolinkMatch> matches = occurrences.stream()
                    .map(o -> new AutolinkMatch(o.targetArticleId(), o.candidateNames(), o.matchedText(),
                            o.occurrenceIndex(), o.snippet()))
                    .toList();
            groups.add(new AutolinkCandidateGroup(article.getId(), article.getTitle(), matches));
        }
        return groups;
    }

    @Override
    @Transactional
    public ArticleView apply(UUID worldId, UUID articleId, List<AutolinkSelection> selections) {
        requireWorld(worldId);
        Article article = articles.findByIdAndWorld(articleId, worldId)
                .orElseThrow(() -> new NotFoundException("Article not found"));
        List<Candidate> candidates = buildCandidates(worldId, articles.findByWorld(worldId));
        Set<Selection> domainSelections = selections.stream()
                .map(s -> new Selection(s.targetArticleId(), s.occurrenceIndex(), s.chosenName()))
                .collect(Collectors.toSet());

        String newBody = AutolinkScanner.apply(article.getBody(), articleId, candidates, domainSelections);
        return updateUseCase.update(new UpdateArticleCommand(worldId, articleId, article.getCategoryId(),
                article.getParentArticleId(), article.getTitle(), article.getSlug(), article.getTemplate(),
                newBody));
    }

    private List<Candidate> buildCandidates(UUID worldId, List<Article> worldArticles) {
        Map<UUID, List<String>> aliasesByArticle = aliases.findAllByWorld(worldId);
        return worldArticles.stream()
                .map(a -> {
                    List<String> names = new ArrayList<>();
                    names.add(a.getTitle());
                    names.addAll(aliasesByArticle.getOrDefault(a.getId(), List.of()));
                    return new Candidate(a.getId(), names);
                })
                .toList();
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }
}
