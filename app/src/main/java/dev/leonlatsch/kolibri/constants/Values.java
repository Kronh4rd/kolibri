package dev.leonlatsch.kolibri.constants;

/**
 * Contains common string and intent keys
 *
 * @author Leon Latsch
 * @since 1.0.0
 */
public class Values {

    // Common
    public static final String EMPTY = "";

    // Keys
    public static final String INTENT_KEY_USERNAME = "intent_username";
    public static final String INTENT_KEY_PROFILE_PIC_UID = "intent_profile_pic_uid";
    public static final String INTENT_KEY_PROFILE_PIC_USERNAME = "intent_profile_pic_username";

    // Contact
    public static final String INTENT_KEY_CONTACT_UID = "intent_contact_uid";

    // Chat keys
    public static final String INTENT_KEY_CHAT_UID = "intent_chat_uid";
    public static final String INTENT_KEY_CHAT_USERNAME = "intent_chat_username";
    public static final String INTENT_KEY_CHAT_PROFILE_PIC = "intent_chat_profile_pic";
    public static final String INTENT_KEY_CHAT_PUBLIC_KEY = "intent_chat_public_key";

    private Values() {
        // Prevent instantiation
    }
}