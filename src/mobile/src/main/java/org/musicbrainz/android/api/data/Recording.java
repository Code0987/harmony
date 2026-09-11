package org.musicbrainz.android.api.data;

/** Minimal stand-in so metadata lookup UI compiles without the MusicBrainz HTTP client. */
public class Recording {
	public String getMbid() {
		return "";
	}

	public String getTitle() {
		return "";
	}

	public java.util.List<ReleaseArtist> getArtists() {
		return java.util.Collections.emptyList();
	}

	public java.util.List<ReleaseInfo> getReleases() {
		return java.util.Collections.emptyList();
	}

	public java.util.List<Tag> getTags() {
		return java.util.Collections.emptyList();
	}
}
