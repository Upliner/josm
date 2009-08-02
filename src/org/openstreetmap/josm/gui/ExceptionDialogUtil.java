// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmTransferException;

/**
 * This utility class provides static methods which explain various exceptions to the user.
 * 
 */
public class ExceptionDialogUtil {

    /**
     * just static utility functions. no constructor
     */
    private ExceptionDialogUtil() {}

    /**
     * handles an exception caught during OSM API initialization
     *
     * @param e the exception
     */
    public static void explainOsmApiInitializationException(OsmApiInitializationException e) {
        e.printStackTrace();
        OptionPaneUtil.showMessageDialog(
                Main.parent,
                tr(   "Failed to initialize communication with the OSM server {0}.\n"
                        + "Check the server URL in your preferences and your internet connection.",
                        Main.pref.get("osm-server.url", "http://api.openstreetmap.org/api")
                ),
                tr("Error"),
                JOptionPane.ERROR_MESSAGE
        );
    }


    /**
     * Explains an upload error due to a violated precondition, i.e. a HTTP return code 412
     *
     * @param e the exception
     */
    public static void explainPreconditionFailed(OsmApiException e) {
        e.printStackTrace();
        OptionPaneUtil.showMessageDialog(
                Main.parent,
                tr("<html>Uploading to the server <strong>failed</strong> because your current<br>"
                        +"dataset violates a precondition.<br>"
                        +"The error message is:<br>"
                        + "{0}"
                        + "</html>",
                        e.getMessage().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                ),
                tr("Precondition violation"),
                JOptionPane.ERROR_MESSAGE
        );
    }


    /**
     * Explains an exception with a generic message dialog
     * 
     * @param e the exception
     */
    public static void explainGeneric(Exception e) {
        String msg = e.getMessage();
        if (msg == null) {
            msg = e.toString();
        }
        e.printStackTrace();
        OptionPaneUtil.showMessageDialog(
                Main.parent,
                msg,
                tr("Error"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Explains a {@see SecurityException} which has caused an {@see OsmTransferException}.
     * This is most likely happening when user tries to access the OSM API from whitin an
     * applet which wasn't loaded from the API server.
     * 
     * @param e the exception
     */

    public static void explainSecurityException(OsmTransferException e) {
        String apiUrl = OsmApi.getOsmApi().getBaseUrl();
        String host = tr("unknown");
        try {
            host = new URL(apiUrl).getHost();
        } catch(MalformedURLException ex) {
            // shouldn't happen
        }

        String message = tr("<html>Failed to open a connection to the remote server<br>"
                + "''{0}''<br>"
                + "for security reasons. This is most likely because you are running<br>"
                + "in an applet and because you didn''t load your applet from ''{1}''.</html>",
                apiUrl, host
        );
        OptionPaneUtil.showMessageDialog(
                Main.parent,
                message,
                tr("Security exception"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Explains a {@see SecurityException} which has caused an {@see OsmTransferException}.
     * This is most likely happening when user tries to access the OSM API from whitin an
     * applet which wasn't loaded from the API server.
     * 
     * @param e the exception
     */

    public static void explainNestedSocketException(OsmTransferException e) {
        String apiUrl = OsmApi.getOsmApi().getBaseUrl();
        String message = tr("<html>Failed to open a connection to the remote server<br>"
                + "''{0}''.<br>"
                + "Please check your internet connection.</html>",
                apiUrl
        );
        e.printStackTrace();
        OptionPaneUtil.showMessageDialog(
                Main.parent,
                message,
                tr("Network exception"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Explains a {@see SecurityException} which has caused an {@see OsmTransferException}.
     * This is most likely happening when user tries to access the OSM API from whitin an
     * applet which wasn't loaded from the API server.
     * 
     * @param e the exception
     */

    public static void explainNestedUnkonwnHostException(OsmTransferException e) {
        String apiUrl = OsmApi.getOsmApi().getBaseUrl();
        String host = tr("unknown");
        try {
            host = new URL(apiUrl).getHost();
        } catch(MalformedURLException ex) {
            // shouldn't happen
        }

        String message = tr("<html>Failed to open a connection to the remote server<br>"
                + "''{0}''.<br>"
                + "Host name ''{1}'' couldn''t be resolved. <br>"
                + "Please check the API URL in your preferences and your internet connection.</html>",
                apiUrl, host
        );
        e.printStackTrace();
        OptionPaneUtil.showMessageDialog(
                Main.parent,
                message,
                tr("Unknown host"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    protected static <T> T getNestedException(Exception e, Class<T> nested) {
        Throwable t = e;
        while (t != null && !(t.getClass().isAssignableFrom(nested))) {
            t = t.getCause();
        }
        return nested.cast(t);
    }

    /**
     * Replies the first {@see SecurityException} in a chain of nested exceptions.
     * null, if no {@see SecurityException} is in this chain.
     * 
     * @param e the root exception
     * @return the first {@see SecurityException} in a chain of nested exceptions
     */
    protected static SecurityException getNestedSecurityException(Exception e) {
        return getNestedException(e, SecurityException.class);
    }


    /**
     * Replies the first {@see SocketException} in a chain of nested exceptions.
     * null, if no {@see SocketException} is in this chain.
     * 
     * @param e the root exception
     * @return the first {@see SocketException} in a chain of nested exceptions
     */
    protected static SocketException getNestedSocketException(Exception e) {
        return getNestedException(e, SocketException.class);
    }

    /**
     * Replies the first {@see UnknownHostException} in a chain of nested exceptions.
     * null, if no {@see UnknownHostException} is in this chain.
     * 
     * @param e the root exception
     * @return the first {@see UnknownHostException} in a chain of nested exceptions
     */
    protected static UnknownHostException getNestedUnknownHostException(Exception e) {
        return getNestedException(e, UnknownHostException.class);
    }


    /**
     * Explains an {@see OsmTransferException} to the user.
     * 
     * @param e the {@see OsmTransferException}
     */
    public static void explainOsmTransferException(OsmTransferException e) {
        if (getNestedSecurityException(e) != null) {
            explainSecurityException(e);
            return;
        }
        if (getNestedSocketException(e) != null) {
            explainNestedSocketException(e);
            return;
        }
        if (getNestedUnknownHostException(e) != null) {
            explainNestedUnkonwnHostException(e);
            return;
        }
        if (e instanceof OsmApiInitializationException){
            explainOsmApiInitializationException((OsmApiInitializationException)e);
            return;
        }
        if (e instanceof OsmApiException) {
            OsmApiException oae = (OsmApiException)e;
            if (oae.getResponseCode() == HttpURLConnection.HTTP_PRECON_FAILED) {
                explainPreconditionFailed(oae);
                return;
            }
            if (oae.getResponseCode() == HttpURLConnection.HTTP_GONE) {
                explainGoneForUnknownPrimitive(oae);
                return;
            }
        }

        explainGeneric(e);
    }

    /**
     * explains the case of an error due to a delete request on an already deleted
     * {@see OsmPrimitive}, i.e. a HTTP response code 410, where we don't know which
     * {@see OsmPrimitive} is causing the error.
     *
     * @param e the exception
     */
    public static void explainGoneForUnknownPrimitive(OsmApiException e) {
        String msg =  tr("<html>Uploading <strong>failed</strong> because a primitive you tried to<br>"
                + "delete on the server is already deleted.<br>"
                + "<br>"
                + "The error message is:<br>"
                + "{0}"
                + "</html>",
                e.getMessage().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        );
        OptionPaneUtil.showMessageDialog(
                Main.parent,
                msg,
                tr("Primitive already deleted"),
                JOptionPane.ERROR_MESSAGE
        );

    }

    /**
     * Explains an {@see Exception} to the user.
     * 
     * @param e the {@see Exception}
     */
    public static void explainException(Exception e) {
        if (e instanceof OsmTransferException) {
            explainOsmTransferException((OsmTransferException)e);
            return;
        }
        explainGeneric(e);
    }
}