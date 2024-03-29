package org.jaudiotagger.tag.id3;

import org.jaudiotagger.tag.id3.framebody.*;

/**
 * List of known id3v23 metadata fields
 *
 * <p>These provide a mapping from the generic key to the underlying ID3v23frames. For example most of the Musicbrainz
 * fields are implemented using a User Defined Text Info Frame, but with a different description key, so this
 * enum provides the link between the two.
 */
public enum ID3v23FieldKey
{
    ALBUM(ID3v23Frames.FRAME_ID_V3_ALBUM, Id3FieldType.TEXT),
    ALBUM_ARTIST(ID3v23Frames.FRAME_ID_V3_ACCOMPANIMENT, Id3FieldType.TEXT),
    ALBUM_ARTIST_SORT(ID3v23Frames.FRAME_ID_V3_ALBUM_ARTIST_SORT_ORDER_ITUNES, Id3FieldType.TEXT),
    ALBUM_SORT(ID3v23Frames.FRAME_ID_V3_ALBUM_SORT_ORDER_ITUNES, Id3FieldType.TEXT),
    AMAZON_ID(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.AMAZON_ASIN, Id3FieldType.TEXT),
    ARTIST(ID3v23Frames.FRAME_ID_V3_ARTIST, Id3FieldType.TEXT),
    ARTIST_SORT(ID3v23Frames.FRAME_ID_V3_ARTIST_SORT_ORDER_ITUNES, Id3FieldType.TEXT),
    BARCODE(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.BARCODE, Id3FieldType.TEXT),
    BPM(ID3v23Frames.FRAME_ID_V3_BPM, Id3FieldType.TEXT),
    CATALOG_NO(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.CATALOG_NO, Id3FieldType.TEXT),
    COMMENT(ID3v23Frames.FRAME_ID_V3_COMMENT, Id3FieldType.TEXT),
    COMPOSER(ID3v23Frames.FRAME_ID_V3_COMPOSER, Id3FieldType.TEXT),
    COMPOSER_SORT(ID3v23Frames.FRAME_ID_V3_COMPOSER_SORT_ORDER_ITUNES, Id3FieldType.TEXT),
    CONDUCTOR(ID3v23Frames.FRAME_ID_V3_CONDUCTOR, Id3FieldType.TEXT),
    COVER_ART(ID3v23Frames.FRAME_ID_V3_ATTACHED_PICTURE, Id3FieldType.BINARY),
    CUSTOM1(ID3v23Frames.FRAME_ID_V3_COMMENT, FrameBodyCOMM.MM_CUSTOM1,Id3FieldType.TEXT),
    CUSTOM2(ID3v23Frames.FRAME_ID_V3_COMMENT, FrameBodyCOMM.MM_CUSTOM2,Id3FieldType.TEXT),
    CUSTOM3(ID3v23Frames.FRAME_ID_V3_COMMENT, FrameBodyCOMM.MM_CUSTOM3,Id3FieldType.TEXT),
    CUSTOM4(ID3v23Frames.FRAME_ID_V3_COMMENT, FrameBodyCOMM.MM_CUSTOM4,Id3FieldType.TEXT),
    CUSTOM5(ID3v23Frames.FRAME_ID_V3_COMMENT, FrameBodyCOMM.MM_CUSTOM5,Id3FieldType.TEXT),
    DISC_NO(ID3v23Frames.FRAME_ID_V3_SET, Id3FieldType.TEXT),
    DISC_SUBTITLE(ID3v23Frames.FRAME_ID_V3_SET_SUBTITLE ,Id3FieldType.TEXT),
    DISC_TOTAL(ID3v23Frames.FRAME_ID_V3_SET, Id3FieldType.TEXT),
    ENCODER(ID3v23Frames.FRAME_ID_V3_ENCODEDBY, Id3FieldType.TEXT),
    FBPM(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.FBPM, Id3FieldType.TEXT),
    GENRE(ID3v23Frames.FRAME_ID_V3_GENRE, Id3FieldType.TEXT),
    GROUPING(ID3v23Frames.FRAME_ID_V3_CONTENT_GROUP_DESC, Id3FieldType.TEXT),
    ISRC(ID3v23Frames.FRAME_ID_V3_ISRC, Id3FieldType.TEXT),
    IS_COMPILATION(ID3v23Frames.FRAME_ID_V3_IS_COMPILATION, Id3FieldType.TEXT),
    KEY(ID3v23Frames.FRAME_ID_V3_INITIAL_KEY,Id3FieldType.TEXT),
    LANGUAGE(ID3v23Frames.FRAME_ID_V3_LANGUAGE,Id3FieldType.TEXT),
    LYRICIST(ID3v23Frames.FRAME_ID_V3_LYRICIST, Id3FieldType.TEXT),
    LYRICS(ID3v23Frames.FRAME_ID_V3_UNSYNC_LYRICS, Id3FieldType.TEXT),
    MEDIA(ID3v23Frames.FRAME_ID_V3_MEDIA_TYPE, Id3FieldType.TEXT),
    MOOD(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MOOD, Id3FieldType.TEXT),
    MUSICBRAINZ_ARTISTID(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSICBRAINZ_ARTISTID, Id3FieldType.TEXT),
    MUSICBRAINZ_DISC_ID(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSICBRAINZ_DISCID, Id3FieldType.TEXT),
    MUSICBRAINZ_ORIGINAL_RELEASEID(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSICBRAINZ_ORIGINAL_ALBUMID, Id3FieldType.TEXT),
    MUSICBRAINZ_RELEASEARTISTID(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSICBRAINZ_ALBUM_ARTISTID, Id3FieldType.TEXT),
    MUSICBRAINZ_RELEASEID(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSICBRAINZ_ALBUMID, Id3FieldType.TEXT),
    MUSICBRAINZ_RELEASE_COUNTRY(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSICBRAINZ_ALBUM_COUNTRY, Id3FieldType.TEXT),
    MUSICBRAINZ_RELEASE_GROUP_ID(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSICBRAINZ_RELEASE_GROUPID, Id3FieldType.TEXT),
    MUSICBRAINZ_RELEASE_TRACK_ID(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSICBRAINZ_RELEASE_TRACKID, Id3FieldType.TEXT),
    MUSICBRAINZ_RELEASE_STATUS(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSICBRAINZ_ALBUM_STATUS, Id3FieldType.TEXT),
    MUSICBRAINZ_RELEASE_TYPE(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSICBRAINZ_ALBUM_TYPE, Id3FieldType.TEXT),
    MUSICBRAINZ_TRACK_ID(ID3v23Frames.FRAME_ID_V3_UNIQUE_FILE_ID, FrameBodyUFID.UFID_MUSICBRAINZ, Id3FieldType.TEXT),
    MUSICBRAINZ_WORK_ID(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSICBRAINZ_WORKID, Id3FieldType.TEXT),
    MUSICIP_ID(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSICIP_ID, Id3FieldType.TEXT),
    OCCASION(ID3v23Frames.FRAME_ID_V3_COMMENT, FrameBodyCOMM.MM_OCCASION,Id3FieldType.TEXT),
    ORIGINAL_ALBUM(ID3v23Frames.FRAME_ID_V3_ORIG_TITLE, Id3FieldType.TEXT),
    ORIGINAL_ARTIST(ID3v23Frames.FRAME_ID_V3_ORIGARTIST, Id3FieldType.TEXT),
    ORIGINAL_LYRICIST(ID3v23Frames.FRAME_ID_V3_ORIG_LYRICIST, Id3FieldType.TEXT),
    ORIGINAL_YEAR(ID3v23Frames.FRAME_ID_V3_TORY, Id3FieldType.TEXT),
    QUALITY(ID3v23Frames.FRAME_ID_V3_COMMENT, FrameBodyCOMM.MM_QUALITY,Id3FieldType.TEXT),
    RATING(ID3v23Frames.FRAME_ID_V3_POPULARIMETER, Id3FieldType.TEXT),
    RECORD_LABEL(ID3v23Frames.FRAME_ID_V3_PUBLISHER, Id3FieldType.TEXT),
    REMIXER(ID3v23Frames.FRAME_ID_V3_REMIXED, Id3FieldType.TEXT),
    SCRIPT(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.SCRIPT, Id3FieldType.TEXT),
    SUBTITLE(ID3v23Frames.FRAME_ID_V3_TITLE_REFINEMENT, Id3FieldType.TEXT),
    TAGS(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.TAGS, Id3FieldType.TEXT),
    TEMPO(ID3v23Frames.FRAME_ID_V3_COMMENT, FrameBodyCOMM.MM_TEMPO,Id3FieldType.TEXT),
    TITLE(ID3v23Frames.FRAME_ID_V3_TITLE, Id3FieldType.TEXT),
    TITLE_SORT(ID3v23Frames.FRAME_ID_V3_TITLE_SORT_ORDER_ITUNES, Id3FieldType.TEXT),
    TRACK(ID3v23Frames.FRAME_ID_V3_TRACK, Id3FieldType.TEXT),
    TRACK_TOTAL(ID3v23Frames.FRAME_ID_V3_TRACK, Id3FieldType.TEXT),
    URL_DISCOGS_ARTIST_SITE(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_URL, FrameBodyWXXX.URL_DISCOGS_ARTIST_SITE, Id3FieldType.TEXT),
    URL_DISCOGS_RELEASE_SITE(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_URL, FrameBodyWXXX.URL_DISCOGS_RELEASE_SITE, Id3FieldType.TEXT),
    URL_LYRICS_SITE(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_URL, FrameBodyWXXX.URL_LYRICS_SITE, Id3FieldType.TEXT),
    URL_OFFICIAL_ARTIST_SITE(ID3v23Frames.FRAME_ID_V3_URL_ARTIST_WEB, Id3FieldType.TEXT),
    URL_OFFICIAL_RELEASE_SITE(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_URL, FrameBodyWXXX.URL_OFFICIAL_RELEASE_SITE, Id3FieldType.TEXT),
    URL_WIKIPEDIA_ARTIST_SITE(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_URL, FrameBodyWXXX.URL_WIKIPEDIA_ARTIST_SITE, Id3FieldType.TEXT),
    URL_WIKIPEDIA_RELEASE_SITE(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_URL, FrameBodyWXXX.URL_WIKIPEDIA_RELEASE_SITE, Id3FieldType.TEXT),
    YEAR(ID3v23Frames.FRAME_ID_V3_TYER, Id3FieldType.TEXT),
    ENGINEER(ID3v23Frames.FRAME_ID_V3_IPLS, FrameBodyTIPL.ENGINEER, Id3FieldType.TEXT),
    PRODUCER(ID3v23Frames.FRAME_ID_V3_IPLS, FrameBodyTIPL.PRODUCER, Id3FieldType.TEXT),
    MIXER(ID3v23Frames.FRAME_ID_V3_IPLS, FrameBodyTIPL.MIXER, Id3FieldType.TEXT),
    DJMIXER(ID3v23Frames.FRAME_ID_V3_IPLS, FrameBodyTIPL.DJMIXER, Id3FieldType.TEXT),
    ARRANGER(ID3v23Frames.FRAME_ID_V3_IPLS, FrameBodyTIPL.ARRANGER, Id3FieldType.TEXT),
    ARTISTS(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.ARTISTS, Id3FieldType.TEXT),
    ACOUSTID_FINGERPRINT(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.ACOUSTID_FINGERPRINT, Id3FieldType.TEXT),
    ACOUSTID_ID(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.ACOUSTID_ID, Id3FieldType.TEXT),
    COUNTRY(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.COUNTRY, Id3FieldType.TEXT),
    LENGTH(ID3v23Frames.FRAME_ID_V3_LENGTH, Id3FieldType.TEXT),
    USER_LYRICS(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.LYRICS, Id3FieldType.TEXT),
    USER_UNSYNCED_LYRICS(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.UNSYNCED_LYRICS, Id3FieldType.TEXT),
    ;
    private String fieldName;

    private String frameId;
    private String subId;
    private Id3FieldType fieldType;

    /**
     * For usual metadata fields that use a data field
     *
     * @param frameId   the frame that will be used
     * @param fieldType of data atom
     */
    ID3v23FieldKey(String frameId, Id3FieldType fieldType)
    {
        this.frameId = frameId;
        this.fieldType = fieldType;

        this.fieldName = frameId;
    }

    /**
     * @param frameId   the frame that will be used
     * @param subId     the additional key required within the frame to uniquely identify this key
     * @param fieldType
     */
    ID3v23FieldKey(String frameId, String subId, Id3FieldType fieldType)
    {
        this.frameId = frameId;
        this.subId = subId;
        this.fieldType = fieldType;

        this.fieldName = frameId + ":" + subId;
    }

    /**
     * @return fieldtype
     */
    public Id3FieldType getFieldType()
    {
        return fieldType;
    }

    /**
     * This is the frame identifier used to write the field
     *
     * @return
     */
    public String getFrameId()
    {
        return frameId;
    }

    /**
     * This is the subfield used within the frame for this type of field
     *
     * @return subId
     */
    public String getSubId()
    {
        return subId;
    }

    /**
     * This is the value of the key that can uniquely identifer a key type
     *
     * @return
     */
    public String getFieldName()
    {
        return fieldName;
    }
}
