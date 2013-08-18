package ch.cyberduck.core.importer;

/*
 * Copyright (c) 2002-2010 David Kocher. All rights reserved.
 *
 * http://cyberduck.ch/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Bug fixes, suggestions and comments should be sent to:
 * dkocher@cyberduck.ch
 */

import ch.cyberduck.core.Filter;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.LocalFactory;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.ProtocolFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @version $Id$
 */
public class WsFtpBookmarkCollection extends ThirdpartyBookmarkCollection {
    private static Logger log = Logger.getLogger(WsFtpBookmarkCollection.class);

    private static final long serialVersionUID = -254244450037887034L;

    @Override
    public String getBundleIdentifier() {
        return "com.ipswitch.wsftp";
    }

    @Override
    public String getName() {
        return "WS_FTP";
    }

    /**
     * @return Folder
     */
    @Override
    public Local getFile() {
        return LocalFactory.createLocal(Preferences.instance().getProperty("bookmark.import.wsftp.location"));
    }

    @Override
    protected void parse(Local folder) {
        for(Local child : folder.list().filter(new Filter<Local>() {
            @Override
            public boolean accept(Local file) {
                if(file.attributes().isDirectory()) {
                    return false;
                }
                return "ini".equals(file.getExtension());
            }
        })) {
            if(child.attributes().isDirectory()) {
                this.parse(child);
            }
            else {
                this.read(child);
            }
        }
    }

    protected void read(Local file) {
        try {
            final BufferedReader in = new BufferedReader(new InputStreamReader(file.getInputStream(), Charset.forName("UTF-8")));
            try {
                Host current = null;
                String line;
                while((line = in.readLine()) != null) {
                    log.trace(line);
                    if(line.startsWith("[")) {
                        this.add(current);

                        current = new Host(Preferences.instance().getProperty("connection.hostname.default"));
                        current.getCredentials().setUsername(
                                Preferences.instance().getProperty("connection.login.anon.name"));
                        current.setProtocol(ProtocolFactory.FTP);
                        Pattern pattern = Pattern.compile("\\[(.*)\\]");
                        Matcher matcher = pattern.matcher(line);
                        if(matcher.matches()) {
                            current.setNickname(matcher.group(1));
                        }
                    }
                    else if(StringUtils.isBlank(line)) {
                        this.add(current);
                    }
                    else {
                        if(null == current) {
                            log.warn("Failed to detect start of bookmark");
                            continue;
                        }
                        this.parse(current, line);
                    }
                }
            }
            finally {
                IOUtils.closeQuietly(in);
            }
        }
        catch(IOException e) {
            log.error(e.getMessage());
        }
    }

    private boolean parse(final Host current, final String line) {
        final Scanner scanner = new Scanner(line);
        scanner.useDelimiter("=");
        if(!scanner.hasNext()) {
            log.warn("Missing key in line:" + line);
            return false;
        }
        String name = scanner.next().toLowerCase(Locale.ENGLISH);
        if(!scanner.hasNext()) {
            log.warn("Missing value in line:" + line);
            return false;
        }
        String value = scanner.next().replaceAll("\"", StringUtils.EMPTY);
        if("conntype".equals(name)) {
            try {
                switch(Integer.parseInt(value)) {
                    case 4:
                        current.setProtocol(ProtocolFactory.SFTP);
                        break;
                    case 5:
                        current.setProtocol(ProtocolFactory.FTP_TLS);
                        break;
                }
            }
            catch(NumberFormatException e) {
                log.warn("Unknown Protocol:" + e.getMessage());
            }
        }
        else if("host".equals(name)) {
            current.setHostname(value);
        }
        else if("port".equals(name)) {
            try {
                current.setPort(Integer.parseInt(value));
            }
            catch(NumberFormatException e) {
                log.warn("Invalid Port:" + e.getMessage());
            }
        }
        else if("dir".equals(name)) {
            current.setDefaultPath(value);
        }
        else if("comment".equals(name)) {
            current.setComment(value);
        }
        else if("uid".equals(name)) {
            current.getCredentials().setUsername(value);
        }
        return true;
    }

    @Override
    public boolean add(Host bookmark) {
        if(bookmark == null) {
            return false;
        }
        if(bookmark.getHostname().equals(
                Preferences.instance().getProperty("connection.hostname.default"))) {
            return false;
        }
        return super.add(bookmark);
    }
}