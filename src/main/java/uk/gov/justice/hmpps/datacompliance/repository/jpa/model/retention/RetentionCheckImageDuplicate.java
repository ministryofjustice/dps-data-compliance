package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageDuplicate;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.FetchType.LAZY;

@Entity
@DiscriminatorValue(RetentionCheckImageDuplicate.IMAGE_DUPLICATE)
public class RetentionCheckImageDuplicate extends RetentionCheck {

    public static final String IMAGE_DUPLICATE = "IMAGE_DUPLICATE";

    @OneToMany(mappedBy = "retentionCheck", cascade = PERSIST, fetch = LAZY)
    private final List<RetentionReasonImageDuplicate> imageDuplicates = new ArrayList<>();

    public RetentionCheckImageDuplicate() {
        this(null);
    }

    public RetentionCheckImageDuplicate(final Status status) {
        super(null, null, IMAGE_DUPLICATE, status);
    }

    public RetentionCheckImageDuplicate addImageDuplicates(final List<ImageDuplicate> imageDuplicates) {
        imageDuplicates.forEach(this::addImageDuplicate);
        return this;
    }

    public RetentionCheckImageDuplicate addImageDuplicate(final ImageDuplicate imageDuplicate) {
        imageDuplicates.add(RetentionReasonImageDuplicate.builder()
                .retentionCheck(this)
                .imageDuplicate(imageDuplicate)
                .build());
        return this;
    }

    public List<ImageDuplicate> getImageDuplicates() {
        return imageDuplicates.stream()
                .map(RetentionReasonImageDuplicate::getImageDuplicate)
                .collect(Collectors.toList());
    }

    @Data
    @Entity
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(of = {"duplicateId"})
    @Table(name = "RETENTION_REASON_IMAGE_DUPLICATE")
    private static class RetentionReasonImageDuplicate {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "RET_REASON_IMG_DUP_ID", nullable = false)
        private Long duplicateId;

        @ManyToOne(fetch = LAZY)
        @JoinColumn(name = "RETENTION_CHECK_ID")
        private RetentionCheckImageDuplicate retentionCheck;

        @ManyToOne(fetch = LAZY)
        @JoinColumn(name = "IMAGE_DUPLICATE_ID")
        private ImageDuplicate imageDuplicate;
    }
}
