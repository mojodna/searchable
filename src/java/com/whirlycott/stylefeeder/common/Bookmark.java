/*
 * Created on Jan 8, 2005 by pjacob
 *
 */
package com.whirlycott.stylefeeder.common;

import static com.whirlycott.stylefeeder.common.Constants.FEMALE_TAG;
import static com.whirlycott.stylefeeder.common.Constants.MALE_TAG;
import static com.whirlycott.stylefeeder.common.Constants.SPACE;
import static com.whirlycott.stylefeeder.common.Constants.UNISEX_TAG;
import static com.whirlycott.stylefeeder.util.TagUtils.convertStringToTagTokens;
import static com.whirlycott.stylefeeder.util.TagUtils.scrubGenderTags;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

import com.whirlycott.stylefeeder.search.Searchable;

/**
 * @author pjacob
 * 
 */
public class Bookmark implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -6199550995404450560L;

    // private Account account;

    @Searchable(Searchable.UNSTORED)
    private boolean anonymous;

    private int comments;

    @Searchable(Searchable.UNSTORED)
    private Date created;

    private Boolean deleted = Boolean.FALSE;

    @Searchable(Searchable.UNSTORED)
    private String description;

    @Searchable(Searchable.KEYWORD)
    private String id;

    // private Link link;

    @Searchable(Searchable.UNSTORED)
    private String rawTags;

    private SortedSet tags;

    @Searchable(Searchable.UNSTORED)
    private String thumbnailStatus;

    @Searchable(Searchable.UNSTORED)
    private String thumbnailUrl;

    @Searchable(Searchable.UNSTORED)
    private Date updated;

    /**
     * 
     */
    public Bookmark() {
        super();
    }

    public String getAbbreviatedDescription() {
        return StringUtils.abbreviate(description, 100);
    }

    public Account getAccount() {
        return account;
    }

    public int getComments() {
        return comments;
    }

    public Date getCreated() {
        return created;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public String getDescription() {
        return description;
    }

    public String getGender() {
        if (tags.contains(MALE_TAG))
            return MALE_TAG.getName();
        if (tags.contains(FEMALE_TAG))
            return FEMALE_TAG.getName();
        // Default.
        return UNISEX_TAG.getName();
    }

    public String getId() {
        return id;
    }

    public Link getLink() {
        return link;
    }

    /**
     * Returns a list of Tag objects in the right order.
     * 
     * @return
     */
    public Set<Tag> getPrintableTags() {
        /*
         * This must be an old bookmark since it doesn't have any raw tag data.
         * At some point, I suppose that this can be deprecated.
         */
        if (StringUtils.isEmpty(rawTags)) {
            final Set<Tag> retval = new TreeSet<Tag>(tags);
            scrubGenderTags(retval);
            return retval;

        } else {
            // TODO - at some point, this can likely be cleaned up by avoiding
            // the nested loop.
            final Set<String> tagTokens = convertStringToTagTokens(rawTags, true);
            final Set<Tag> retval = new LinkedHashSet<Tag>();
            for (final String tagName : tagTokens) {
                for (final Tag t : tags) {
                    if (tagName.equals(t.getName())) {
                        retval.add(t);
                        break;
                    }
                }
            }
            return retval;
        }

    }

    public String getPrintableTagsAsString() {
        final StringBuffer sb = new StringBuffer();
        for (final Tag tag : getPrintableTags()) {
            sb.append(tag.getName());
            sb.append(SPACE);
        }
        return sb.toString();
    }

    public String getRawTags() {
        return rawTags;
    }

    public SortedSet<Tag> getTags() {
        return tags;
    }

    public String getThumbnailStatus() {
        return thumbnailStatus;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public Date getUpdated() {
        return updated;
    }

    public synchronized void increaseCommentCount() {
        setComments(getComments() + 1);
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAccount(final Account owner) {
        this.account = owner;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public void setComments(int comments) {
        this.comments = comments;
    }

    public void setCreated(final Date created) {
        this.created = created;
    }

    public void setDeleted(final Boolean deleted) {
        this.deleted = deleted;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setLink(final Link url) {
        this.link = url;
    }

    public void setRawTags(String rawTags) {
        this.rawTags = rawTags;
    }

    public void setTags(final SortedSet<Tag> tags) {
        this.tags = tags;
    }

    public void setThumbnailStatus(String thumbnail) {
        this.thumbnailStatus = thumbnail;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
    
    public void setUpdated(final Date updated) {
        this.updated = updated;
    }

}