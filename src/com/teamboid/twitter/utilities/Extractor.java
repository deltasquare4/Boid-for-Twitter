package com.teamboid.twitter.utilities;

import java.util.*;
import java.util.regex.Matcher;

/**
 * @author Twitter
 */
public class Extractor {
	public static class Entity {
		public enum Type {
			URL, HASHTAG, MENTION, SEARCH
		}
		protected int start;
		protected int end;
		protected final String value;
		protected final String listSlug;
		protected final Type type;

		protected String displayURL = null;
		protected String expandedURL = null;

		public Entity(int start, int end, String value, String listSlug, Type type) {
			this.start = start;
			this.end = end;
			this.value = value;
			this.listSlug = listSlug;
			this.type = type;
		}
		public Entity(int start, int end, String value, Type type) {
			this(start, end, value, null, type);
		}
		public Entity(Matcher matcher, Type type, int groupNumber) {
			this(matcher, type, groupNumber, -1);
		}
		public Entity(Matcher matcher, Type type, int groupNumber, int startOffset) {
			this(matcher.start(groupNumber) + startOffset, matcher.end(groupNumber), matcher.group(groupNumber), type);
		}
		public boolean equals(Object obj) {
			if(this == obj) return true;
			if(!(obj instanceof Entity)) return false;
			Entity other = (Entity)obj;
			if (this.type.equals(other.type) && this.start == other.start && this.end == other.end && this.value.equals(other.value)) {
				return true;
			} else return false;
		}
		public int hashCode() { return this.type.hashCode() + this.value.hashCode() + this.start + this.end; }
		public Integer getStart() {
			return start;
		}
		public Integer getEnd() {
			return end;
		}
		public String getValue() {
			return value;
		}
		public String getListSlug() {
			return listSlug;
		}
		public Type getType() {
			return type;
		}
		public String getDisplayURL() {
			return displayURL;
		}
		public void setDisplayURL(String displayURL) {
			this.displayURL = displayURL;
		}
		public String getExpandedURL() {
			return expandedURL;
		}
		public void setExpandedURL(String expandedURL) {
			this.expandedURL = expandedURL;
		}
	}
	protected boolean extractURLWithoutProtocol = true;
	public Extractor() {
	}

	private void removeOverlappingEntities(List<Entity> entities) {
		Collections.<Entity>sort(entities, new Comparator<Entity>() {
			public int compare(Entity e1, Entity e2) {
				return e1.start - e2.start;
			}
		});
		if(!entities.isEmpty()) {
			Iterator<Entity> it = entities.iterator();
			Entity prev = it.next();
			while (it.hasNext()) {
				Entity cur = it.next();
				if (prev.getEnd() > cur.getStart()) it.remove();
				else prev = cur;
			}
		}
	}
	public List<Entity> extractEntitiesWithIndices(String text) {
		List<Entity> entities = new ArrayList<Entity>();
		entities.addAll(extractURLsWithIndices(text));
		entities.addAll(extractHashtagsWithIndices(text, false));
		entities.addAll(extractMentionsOrListsWithIndices(text));
		entities.addAll(extractSearchesWithIndices(text));
		removeOverlappingEntities(entities);
		return entities;
	}
	public List<String> extractMentionedScreennames(String text) {
		if(text == null || text.trim().length() == 0) return Collections.emptyList();
		List<String> extracted = new ArrayList<String>();
		for(Entity entity : extractMentionedScreennamesWithIndices(text)) extracted.add(entity.value);
		return extracted;
	}
	public List<Entity> extractMentionedScreennamesWithIndices(String text) {
		List<Entity> extracted = new ArrayList<Entity>();
		for(Entity entity : extractMentionsOrListsWithIndices(text)) {
			if(entity.listSlug == null) extracted.add(entity);
		}
		return extracted;
	}
	public List<Entity> extractMentionsOrListsWithIndices(String text) {
		if(text == null || text.trim().length() == 0) return Collections.emptyList();
		boolean found = false;
		for (char c : text.toCharArray()) {
			if (c == '@') {
				found = true;
				break;
			}
		}
		if(!found) return Collections.emptyList();
		List<Entity> extracted = new ArrayList<Entity>();
		Matcher matcher = Regex.VALID_MENTION_OR_LIST.matcher(text);
		while (matcher.find()) {
			String after = text.substring(matcher.end());
			if (!Regex.INVALID_MENTION_MATCH_END.matcher(after).find()) {
				if (matcher.group(Regex.VALID_MENTION_OR_LIST_GROUP_LIST) == null) {
					extracted.add(new Entity(matcher, Entity.Type.MENTION, Regex.VALID_MENTION_OR_LIST_GROUP_USERNAME));
				} else {
					extracted.add(new Entity(matcher.start(Regex.VALID_MENTION_OR_LIST_GROUP_USERNAME) - 1,
							matcher.end(Regex.VALID_MENTION_OR_LIST_GROUP_LIST),
							matcher.group(Regex.VALID_MENTION_OR_LIST_GROUP_USERNAME),
							matcher.group(Regex.VALID_MENTION_OR_LIST_GROUP_LIST),
							Entity.Type.MENTION));
				}
			}
		}
		return extracted;
	}
	public String extractReplyScreenname(String text) {
		if (text == null) return null;
		Matcher matcher = Regex.VALID_REPLY.matcher(text);
		if (matcher.find()) {
			String after = text.substring(matcher.end());
			if (Regex.INVALID_MENTION_MATCH_END.matcher(after).find()) return null;
			else return matcher.group(Regex.VALID_REPLY_GROUP_USERNAME);
		} else return null;
	}
	public List<String> extractURLs(String text) {
		if(text == null || text.trim().length() == 0) return Collections.emptyList();
		List<String> urls = new ArrayList<String>();
		for(Entity entity : extractURLsWithIndices(text)) urls.add(entity.value);
		return urls;
	}
	public List<Entity> extractURLsWithIndices(String text) {
		if (text == null || text.trim().length() == 0 || (extractURLWithoutProtocol ? text.indexOf('.') : text.indexOf(':')) == -1) {
			return Collections.emptyList();
		}
		List<Entity> urls = new ArrayList<Entity>();
		Matcher matcher = Regex.VALID_URL.matcher(text);
		while (matcher.find()) {
			if (matcher.group(Regex.VALID_URL_GROUP_PROTOCOL) == null) {
				if (!extractURLWithoutProtocol || Regex.INVALID_URL_WITHOUT_PROTOCOL_MATCH_BEGIN.matcher(matcher.group(Regex.VALID_URL_GROUP_BEFORE)).matches()) {
					continue;
				}
			}
			String url = matcher.group(Regex.VALID_URL_GROUP_URL);
			int start = matcher.start(Regex.VALID_URL_GROUP_URL);
			int end = matcher.end(Regex.VALID_URL_GROUP_URL);
			Matcher tco_matcher = Regex.VALID_TCO_URL.matcher(url);
			if (tco_matcher.find()) {
				url = tco_matcher.group();
				end = start + url.length();
			}
			urls.add(new Entity(start, end, url, Entity.Type.URL));
		}

		return urls;
	}
	public List<String> extractHashtags(String text) {
		if (text == null || text.trim().length() == 0) return Collections.emptyList();
		List<String> extracted = new ArrayList<String>();
		for(Entity entity : extractHashtagsWithIndices(text)) extracted.add(entity.value);
		return extracted;
	}
	public List<String> extractSearches(String text) {
		if (text == null || text.trim().length() == 0) return Collections.emptyList();
		List<String> extracted = new ArrayList<String>();
		for(Entity entity : extractSearchesWithIndices(text)) extracted.add(entity.value);
		return extracted;
	}
	public List<Entity> extractHashtagsWithIndices(String text) {
		return extractHashtagsWithIndices(text, true);
	}	
	public List<Entity> extractSearchesWithIndices(String text) {
		return extractSearchesWithIndices(text, true);
	}
	private List<Entity> extractHashtagsWithIndices(String text, boolean checkUrlOverlap) {
		if(text == null || text.trim().length() == 0) return Collections.emptyList();
		boolean found = false;
		for (char c : text.toCharArray()) {
			if (c == '#') {
				found = true;
				break;
			}
		}
		if(!found) return Collections.emptyList();
		List<Entity> extracted = new ArrayList<Entity>();
		Matcher matcher = Regex.VALID_HASHTAG.matcher(text);
		while (matcher.find()) {
			String after = text.substring(matcher.end());
			if (!Regex.INVALID_HASHTAG_MATCH_END.matcher(after).find()) {
				extracted.add(new Entity(matcher, Entity.Type.HASHTAG, Regex.VALID_HASHTAG_GROUP_TAG));
			}
		}
		if (checkUrlOverlap) {
			List<Entity> urls = extractURLsWithIndices(text);
			if (!urls.isEmpty()) {
				extracted.addAll(urls);
				removeOverlappingEntities(extracted);
				Iterator<Entity> it = extracted.iterator();
				while (it.hasNext()) {
					Entity entity = it.next();
					if (entity.getType() != Entity.Type.HASHTAG) it.remove();
				}
			}
		}
		return extracted;
	}
	private List<Entity> extractSearchesWithIndices(String text, boolean checkUrlOverlap) {
		if(text == null || text.trim().length() == 0) return Collections.emptyList();
		boolean found = false;
		for (char c : text.toCharArray()) {
			if (c == '$') {
				found = true;
				break;
			}
		}
		if(!found) return Collections.emptyList();
		List<Entity> extracted = new ArrayList<Entity>();
		Matcher matcher = Regex.VALID_SEARCH.matcher(text);
		while (matcher.find()) {
			String after = text.substring(matcher.end());
			if (!Regex.INVALID_HASHTAG_MATCH_END.matcher(after).find()) {
				extracted.add(new Entity(matcher, Entity.Type.SEARCH, Regex.VALID_HASHTAG_GROUP_TAG));
			}
		}
		if (checkUrlOverlap) {
			List<Entity> urls = extractURLsWithIndices(text);
			if (!urls.isEmpty()) {
				extracted.addAll(urls);
				removeOverlappingEntities(extracted);
				Iterator<Entity> it = extracted.iterator();
				while (it.hasNext()) {
					Entity entity = it.next();
					if (entity.getType() != Entity.Type.HASHTAG) {
						it.remove();
					}
				}
			}
		}
		return extracted;
	}
	public void setExtractURLWithoutProtocol(boolean extractURLWithoutProtocol) {
		this.extractURLWithoutProtocol = extractURLWithoutProtocol;
	}
	public boolean isExtractURLWithoutProtocol() {
		return extractURLWithoutProtocol;
	}
	public void modifyIndicesFromUnicodeToUTF16(String text, List<Entity> entities) {
		IndexConverter convert = new IndexConverter(text);
		for (Entity entity : entities) {
			entity.start = convert.codePointsToCodeUnits(entity.start);
			entity.end = convert.codePointsToCodeUnits(entity.end);
		}
	}
	public void modifyIndicesFromUTF16ToToUnicode(String text, List<Entity> entities) {
		IndexConverter convert = new IndexConverter(text);
		for (Entity entity : entities) {
			entity.start = convert.codeUnitsToCodePoints(entity.start);
			entity.end = convert.codeUnitsToCodePoints(entity.end);
		}
	}
	private static final class IndexConverter {
		protected final String text;
		protected int codePointIndex = 0;
		protected int charIndex = 0;
		IndexConverter(String text) {
			this.text = text;
		}
		int codeUnitsToCodePoints(int charIndex) {
			if (charIndex < this.charIndex) codePointIndex -= text.codePointCount(charIndex, this.charIndex);
			else codePointIndex += text.codePointCount(this.charIndex, charIndex);
			this.charIndex = charIndex;
			if (charIndex > 0 && Character.isSupplementaryCodePoint(text.codePointAt(charIndex - 1))) {
				this.charIndex -= 1;
			}
			return this.codePointIndex;
		}
		int codePointsToCodeUnits(int codePointIndex) {
			this.charIndex = text.offsetByCodePoints(this.charIndex, codePointIndex - this.codePointIndex);
			this.codePointIndex = codePointIndex;
			return this.charIndex;
		}
	}
}
