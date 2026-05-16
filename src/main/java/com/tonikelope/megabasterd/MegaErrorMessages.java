/*
 __  __                  _               _               _
|  \/  | ___  __ _  __ _| |__   __ _ ___| |_ ___ _ __ __| |
| |\/| |/ _ \/ _` |/ _` | '_ \ / _` / __| __/ _ \ '__/ _` |
| |  | |  __/ (_| | (_| | |_) | (_| \__ \ ||  __/ | | (_| |
|_|  |_|\___|\__, |\__,_|_.__/ \__,_|___/\__\___|_|  \__,_|
             |___/
© Perpetrated by tonikelope since 2016
 */
package com.tonikelope.megabasterd;

import java.awt.Component;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.JOptionPane;

/**
 * Human-friendly mapping of MEGA API error codes to short name + meaning +
 * suggested action, plus a JOptionPane helper that pops a dialog explaining
 * what the user is looking at. Previously these errors surfaced as raw "FATAL
 * ERROR! MEGA API ERROR: -16" strings on row status labels (or silently as a
 * null quota) and the user had no idea whether the cause was MegaBasterd, the
 * link, the account, or MEGA's server.
 *
 * The mapping follows the codes documented at
 * https://github.com/meganz/sdk/blob/master/include/mega/error.h .
 *
 * @author tonikelope
 */
public final class MegaErrorMessages {

    private MegaErrorMessages() {
    }

    /**
     * Dedup window: never show the same (code, identity) popup more than once
     * every 60 s. Avoids spamming the user when a refresh loop or a worker is
     * retrying.
     */
    private static final long POPUP_DEDUP_WINDOW_MS = 60_000L;
    private static final ConcurrentHashMap<String, Long> LAST_POPUP_TS = new ConcurrentHashMap<>();

    public static String getShortName(int code) {
        switch (code) {
            case -1:
                return "EINTERNAL";
            case -2:
                return "EARGS";
            case -3:
                return "EAGAIN";
            case -4:
                return "ERATELIMIT";
            case -5:
                return "EFAILED";
            case -6:
                return "ETOOMANY";
            case -7:
                return "ERANGE";
            case -8:
                return "EEXPIRED";
            case -9:
                return "ENOENT";
            case -10:
                return "ECIRCULAR";
            case -11:
                return "EACCESS";
            case -12:
                return "EEXIST";
            case -13:
                return "EINCOMPLETE";
            case -14:
                return "EKEY";
            case -15:
                return "ESID";
            case -16:
                return "EBLOCKED";
            case -17:
                return "EOVERQUOTA";
            case -18:
                return "ETEMPUNAVAIL";
            case -19:
                return "ETOOMANYCONNECTIONS";
            case -20:
                return "EWRITE";
            case -21:
                return "EREAD";
            case -22:
                return "EAPPKEY";
            case -23:
                return "ESSL";
            case -24:
                return "EGOINGOVERQUOTA";
            case -25:
                return "EMFAREQUIRED";
            case -26:
                return "EMASTERONLY";
            default:
                return "UNKNOWN";
        }
    }

    public static String getMeaning(int code) {
        switch (code) {
            case -1:
                return "Internal MEGA server error.";
            case -2:
                return "Bad arguments sent to MEGA (likely an internal MegaBasterd bug).";
            case -3:
                return "MEGA is asking us to retry later (server-side overload).";
            case -4:
                return "Too many requests to MEGA from your IP (rate limit).";
            case -5:
                return "MEGA reported a permanent failure for this request.";
            case -6:
                return "Too many concurrent IP addresses on this account.";
            case -7:
                return "Requested range is out of bounds for this resource.";
            case -8:
                return "The MEGA link has expired and is no longer accessible.";
            case -9:
                return "The file or folder was not found on MEGA (may have been deleted by the owner).";
            case -10:
                return "Circular linkage detected.";
            case -11:
                return "Access denied by MEGA -- this account does not have permission for the requested resource.";
            case -12:
                return "Resource already exists.";
            case -13:
                return "Operation incomplete.";
            case -14:
                return "Wrong password or invalid decryption key for this MEGA link.";
            case -15:
                return "Your MEGA session was invalidated (logged out remotely, expired, or the account was modified server-side).";
            case -16:
                return "This MEGA account or resource is currently BLOCKED by MEGA. The block can be either (a) a temporary SECURITY HOLD -- e.g. MEGA detected an unusual login attempt and froze the account until you confirm it; or (b) a permanent ACCOUNT BAN (Terms of Service violation, suspicious activity, throwaway-email detection). MegaBasterd cannot tell which from the API error alone -- the cause is only visible if you log into mega.nz in your browser. This is enforced by MEGA's servers, NOT by MegaBasterd.";
            case -17:
                return "This MEGA account is OVER its storage or bandwidth quota.";
            case -18:
                return "Resource temporarily not available; try again later.";
            case -19:
                return "Too many concurrent connections from your IP.";
            case -20:
                return "MEGA could not write to the file.";
            case -21:
                return "MEGA could not read from the file.";
            case -22:
                return "Invalid or missing application key.";
            case -23:
                return "SSL verification failed.";
            case -24:
                return "This action would push the account over its storage quota.";
            case -25:
                return "MEGA requires multi-factor authentication for this account.";
            case -26:
                return "This action requires being logged into a MEGA account.";
            default:
                return "Generic MEGA API error.";
        }
    }

    public static String getSuggestion(int code) {
        switch (code) {
            case -1:
            case -3:
            case -5:
            case -18:
                return "Wait a few minutes and try again. If it persists, MEGA's servers may be having issues.";
            case -2:
                return "Verify the MEGA link is complete and correctly pasted. If you copied it from MegaBasterd, file a bug report.";
            case -4:
            case -19:
                return "You are sending too many requests. Reduce concurrent transfers, or wait a few minutes for the rate limit to clear.";
            case -6:
                return "MEGA blocks accounts being used from too many IPs at once. Close MegaBasterd on other machines, or use a different account.";
            case -8:
                return "The link has expired -- the owner needs to re-upload and share a new one.";
            case -9:
                return "Verify the link is correct. The file may have been deleted by its owner.";
            case -11:
                return "Verify the link is for a public resource. If this is your account, re-login from Settings -> Accounts.";
            case -14:
                return "Re-check the password used to decrypt this MEGA link.";
            case -15:
                return "Re-login the affected account from Settings -> Accounts.";
            case -16:
                return "Open mega.nz in a browser and log into this account: the web client will show the actual cause. If it's a security hold you can usually clear it by following the on-screen prompts (re-confirm email, change password). If the account is permanently banned, try a different account.";
            case -17:
            case -24:
                return "Free up space in this MEGA account, or upload to a different one.";
            case -25:
                return "MegaBasterd will prompt for your 2FA code -- enter it when asked.";
            case -26:
                return "Log into the MEGA account from Settings -> Accounts before attempting this.";
            default:
                return "Retry the operation. If it persists, try a different account or a different link.";
        }
    }

    public static boolean isAccountBlocked(int code) {
        return code == -16;
    }

    public static boolean isAccessDenied(int code) {
        return code == -11;
    }

    public static boolean isQuotaIssue(int code) {
        return code == -17 || code == -24 || code == -4;
    }

    public static boolean isSessionDead(int code) {
        return code == -15;
    }

    public static boolean isLinkBroken(int code) {
        return code == -8 || code == -9 || code == -14;
    }

    public static String getTitle(int code) {
        if (isAccountBlocked(code)) {
            return "MEGA account blocked";
        }
        if (isQuotaIssue(code)) {
            return "MEGA quota issue";
        }
        if (isSessionDead(code)) {
            return "MEGA session invalid";
        }
        if (isLinkBroken(code)) {
            return "MEGA link unavailable";
        }
        return "MEGA API error " + code;
    }

    /**
     * Build the HTML body for the popup. Public so callers that want to log the
     * human-friendly text (instead of showing a dialog) can reuse it.
     */
    public static String buildPopupHtml(int code, String identity, String context) {
        StringBuilder sb = new StringBuilder("<html><div style='width: 460px'>");
        sb.append("<b style='font-size:13px'>").append(getTitle(code)).append("</b><br><br>");
        if (identity != null && !identity.isEmpty()) {
            sb.append("<i>").append(escapeHtml(identity)).append("</i><br><br>");
        }
        sb.append("<b>Error code:</b> ").append(code).append(" (").append(getShortName(code)).append(")<br><br>");
        sb.append("<b>What this means:</b><br>").append(escapeHtml(getMeaning(code))).append("<br><br>");
        sb.append("<b>What you can do:</b><br>").append(escapeHtml(getSuggestion(code)));
        if (context != null && !context.isEmpty()) {
            sb.append("<br><br><font color='gray'><i>Source: ").append(escapeHtml(context)).append("</i></font>");
        }
        if (!isAccountBlocked(code) && !isQuotaIssue(code)) {
            // Reassure users that the well-known fatal-but-not-our-fault errors
            // aren't a MegaBasterd bug. -16 / -17 are nearly always
            // "the cause is MEGA, not us".
        } else {
            sb.append("<br><br><font color='gray'><i>This error is enforced by MEGA's servers -- it is not a MegaBasterd bug.</i></font>");
        }
        sb.append("</div></html>");
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Show a JOptionPane explaining a MEGA API error code. Deduplicates so the
     * same (code, identity) doesn't pop twice within
     * {@link #POPUP_DEDUP_WINDOW_MS}. Always invoked on the EDT.
     *
     * @param parent Dialog parent (may be null).
     * @param code MEGA API error code (negative).
     * @param identity Account email / link / file name this error applies to.
     * Shown in italics under the title.
     * @param context Free-text context to display in small grey. E.g. "while
     * checking account quota" or "during chunk upload".
     */
    public static void showPopup(Component parent, int code, String identity, String context) {
        String dedup_key = code + "|" + (identity == null ? "" : identity);
        long now = System.currentTimeMillis();
        Long prev = LAST_POPUP_TS.get(dedup_key);
        if (prev != null && now - prev < POPUP_DEDUP_WINDOW_MS) {
            return;
        }
        LAST_POPUP_TS.put(dedup_key, now);

        int msg_type = isAccountBlocked(code) || isLinkBroken(code)
                ? JOptionPane.ERROR_MESSAGE
                : JOptionPane.WARNING_MESSAGE;

        String body = buildPopupHtml(code, identity, context);
        String title = getTitle(code);

        MiscTools.GUIRun(() -> JOptionPane.showMessageDialog(parent, body, title, msg_type));
    }

    /**
     * Convenience: extract the numeric code from a MegaAPIException message
     * (which is always shaped "MEGA API ERROR: -N" or "MEGA API ERROR: -N extra
     * info..."). Returns 0 if it can't parse. Useful when callers caught a
     * generic Exception and only kept its message.
     */
    public static int parseCodeFromMessage(String message) {
        if (message == null) {
            return 0;
        }
        String m = MiscTools.findFirstRegex("MEGA API ERROR:\\s*(-?\\d+)", message, 1);
        if (m == null) {
            return 0;
        }
        try {
            return Integer.parseInt(m);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
