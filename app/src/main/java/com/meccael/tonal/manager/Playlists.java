package com.meccael.tonal.manager;

import android.content.Context;
import android.util.Log;
import android.util.Xml;
import com.bumptech.glide.load.Key;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;
import com.meccael.tonal.application.ServiceMusic;

/* JADX INFO: loaded from: classes.dex */
public class Playlists {
    private static String TAG = "TONAL";
    private static String ITEM = "music";
    private static String MUSIC_ID = "id";
    private static String MUSIC_COVER = "cover";
    private static String MUSIC_DURATION = "duration";
    private static String MUSIC_DATE = "date";
    private static String MUSIC_FILE = "file";
    private static String MUSIC_TITLE = "title";
    private static String MUSIC_ARTIST = "artist";
    private static String MUSIC_ALBUM = "album";

    public static void delete(Context context, String str, ServiceMusic serviceMusic) {
        Log.i(TAG, "Delete playlist " + str);
        if (new File(context.getFilesDir(), str + ".xml").delete()) {
            Log.i(TAG, "Playlist deleted.");
        } else {
            Log.e(TAG, "Unable to delete playlist.");
        }
        new Manager().updatePlaylists(context, serviceMusic);
    }

    private static Document getDomElement(String str) {
        try {
            DocumentBuilder documentBuilderNewDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(str));
            return documentBuilderNewDocumentBuilder.parse(inputSource);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            Log.e(TAG, "Unable to parse the playlist.");
            return null;
        }
    }

    private static String getElementValue(Node node) {
        if (node != null && node.hasChildNodes()) {
            for (Node firstChild = node.getFirstChild(); firstChild != null; firstChild = firstChild.getNextSibling()) {
                if (firstChild.getNodeType() == 3) {
                    return firstChild.getNodeValue();
                }
            }
        }
        return null;
    }

    private static String getFileContent(Context context, String str) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(context.openFileInput(str)));
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    bufferedReader.close();
                    return sb.toString();
                }
                sb.append(line);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Unable to find the playlist " + str);
            return null;
        } catch (IOException e2) {
            Log.e(TAG, "Unable to read the playlist " + str);
            return null;
        }
    }

    private static Long getLong(Element element, String str) {
        return Long.valueOf(Long.parseLong(getElementValue(element.getElementsByTagName(str).item(0))));
    }

    private static String getString(Element element, String str) {
        return getElementValue(element.getElementsByTagName(str).item(0));
    }

    public static Playlist load(Context context, String str, int i) {
        Document domElement;
        NodeList elementsByTagName;
        Log.i(TAG, "Load playlist " + str);
        String fileContent = getFileContent(context, str);
        if (fileContent == null || (domElement = getDomElement(fileContent)) == null || (elementsByTagName = domElement.getElementsByTagName(ITEM)) == null) {
            return null;
        }
        ArrayList arrayList = new ArrayList(elementsByTagName.getLength());
        long j = 0;
        int i2 = 0;
        while (i2 < elementsByTagName.getLength()) {
            Element element = (Element) elementsByTagName.item(i2);
            long jLongValue = j + getLong(element, MUSIC_DURATION).longValue();
            arrayList.add(new Music(getLong(element, MUSIC_ID).longValue(), getLong(element, MUSIC_COVER).longValue(), getLong(element, MUSIC_DURATION).longValue(), getLong(element, MUSIC_DATE).longValue(), getString(element, MUSIC_FILE), getString(element, MUSIC_TITLE), getString(element, MUSIC_ARTIST), getString(element, MUSIC_ALBUM)));
            i2++;
            j = jLongValue;
        }
        return new Playlist(str.substring(0, str.length() - 4), Integer.valueOf(String.valueOf(6) + String.valueOf(i)).intValue(), ((Music) arrayList.get(0)).getCover(), j, arrayList);
    }

    public static void save(Context context, String str, int i, ServiceMusic serviceMusic, List<Music> list) {
        Log.i(TAG, "Save playlist " + str + ".");
        XmlSerializer xmlSerializerNewSerializer = Xml.newSerializer();
        StringWriter stringWriter = new StringWriter();
        try {
            xmlSerializerNewSerializer.setOutput(stringWriter);
            xmlSerializerNewSerializer.startDocument(Key.STRING_CHARSET_NAME, true);
            xmlSerializerNewSerializer.startTag("", "playlist" + Integer.toString(i));
            for (Music music : list) {
                xmlSerializerNewSerializer.startTag("", ITEM);
                xmlSerializerNewSerializer.startTag("", MUSIC_ID);
                xmlSerializerNewSerializer.text(Long.toString(music.getID()));
                xmlSerializerNewSerializer.endTag("", MUSIC_ID);
                xmlSerializerNewSerializer.startTag("", MUSIC_COVER);
                xmlSerializerNewSerializer.text(Long.toString(music.getCover()));
                xmlSerializerNewSerializer.endTag("", MUSIC_COVER);
                xmlSerializerNewSerializer.startTag("", MUSIC_DURATION);
                xmlSerializerNewSerializer.text(Long.toString(music.getDuration()));
                xmlSerializerNewSerializer.endTag("", MUSIC_DURATION);
                xmlSerializerNewSerializer.startTag("", MUSIC_DATE);
                xmlSerializerNewSerializer.text(Long.toString(music.getDate()));
                xmlSerializerNewSerializer.endTag("", MUSIC_DATE);
                xmlSerializerNewSerializer.startTag("", MUSIC_FILE);
                xmlSerializerNewSerializer.text(music.getFile());
                xmlSerializerNewSerializer.endTag("", MUSIC_FILE);
                xmlSerializerNewSerializer.startTag("", MUSIC_TITLE);
                xmlSerializerNewSerializer.text(music.getTitle());
                xmlSerializerNewSerializer.endTag("", MUSIC_TITLE);
                xmlSerializerNewSerializer.startTag("", MUSIC_ARTIST);
                xmlSerializerNewSerializer.text(music.getArtist());
                xmlSerializerNewSerializer.endTag("", MUSIC_ARTIST);
                xmlSerializerNewSerializer.startTag("", MUSIC_ALBUM);
                xmlSerializerNewSerializer.text(music.getAlbum());
                xmlSerializerNewSerializer.endTag("", MUSIC_ALBUM);
                xmlSerializerNewSerializer.endTag("", ITEM);
            }
            xmlSerializerNewSerializer.endTag("", "playlist" + Integer.toString(i));
            xmlSerializerNewSerializer.endDocument();
            String string = stringWriter.toString();
            if (string == null || string.isEmpty()) {
                Log.e(TAG, "The current playlist is empty, creation failed.");
                return;
            }
            try {
                FileOutputStream fileOutputStreamOpenFileOutput = context.openFileOutput(str + ".xml", 0);
                fileOutputStreamOpenFileOutput.write(string.getBytes());
                fileOutputStreamOpenFileOutput.close();
                Log.i(TAG, "The playlist " + str + " has been successfully created.");
                new Manager().updatePlaylists(context, serviceMusic);
            } catch (Exception e) {
                Log.e(TAG, "An error while creating playlist file has occurred.");
                e.printStackTrace();
            }
        } catch (Exception e2) {
            Log.e(TAG, "Cannot parse the current playlist.");
            throw new RuntimeException(e2);
        }
    }
}
