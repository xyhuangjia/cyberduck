package ch.cyberduck.core;

/*
 * Copyright (c) 2012 David Kocher. All rights reserved.
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

import ch.cyberduck.core.cryptomator.LookupVault;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.ssl.DefaultTrustManagerHostnameCallback;
import ch.cyberduck.core.ssl.KeychainX509KeyManager;
import ch.cyberduck.core.ssl.KeychainX509TrustManager;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public final class SessionFactory {
    private static final Logger log = Logger.getLogger(SessionFactory.class);

    private SessionFactory() {
        //
    }

    public static Session<?> create(final Host host, final X509TrustManager trust, final X509KeyManager key,
                                    final PasswordStore keychain, final PasswordCallback login) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Create session for %s", host));
        }
        final Protocol protocol = host.getProtocol();
        final String prefix = protocol.getPrefix();

        try {
            final Class<Session> name = (Class<Session>) Class.forName(String.format("%sSession", prefix));
            final Constructor<Session> constructor = ConstructorUtils.getMatchingAccessibleConstructor(name,
                    host.getClass(), trust.getClass(), key.getClass());
            final Session<?> session;
            if(null == constructor) {
                log.warn(String.format("No matching constructor for parameter %s, %s, %s", host.getClass(), trust.getClass(), key.getClass()));
                final Constructor<Session> fallback = ConstructorUtils.getMatchingAccessibleConstructor(name,
                        host.getClass());
                if(fallback == null) {
                    throw new FactoryException(String.format("No matching constructor for parameter %s", host.getClass()));
                }
                session = fallback.newInstance(host);
            }
            else {
                session = constructor.newInstance(host, trust, key);
            }
            if(PreferencesFactory.get().getBoolean("cryptomator.enable")) {
                return session.withVault(new LookupVault(keychain, login));
            }
            return session;
        }
        catch(InstantiationException | InvocationTargetException | ClassNotFoundException | IllegalAccessException e) {
            throw new FactoryException(String.format("Failure loading session class for %s protocol. Failure %s", protocol, e));
        }
    }

    public static Session<?> create(final Host target) {
        return create(target, new KeychainX509TrustManager(new DefaultTrustManagerHostnameCallback(target)),
                new KeychainX509KeyManager(target), new DisabledPasswordStore(), new DisabledPasswordCallback());
    }
}
