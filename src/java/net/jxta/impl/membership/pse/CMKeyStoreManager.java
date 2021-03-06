/************************************************************************
 *
 * $Id: CMKeyStoreManager.java,v 1.1 2007/01/16 11:01:49 thomas Exp $
 *
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *       Sun Microsystems, Inc. for Project JXTA."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact Project JXTA at http://www.jxta.org.
 *
 * 5. Products derived from this software may not be called "JXTA",
 *    nor may "JXTA" appear in their name, without prior written
 *    permission of Sun.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *********************************************************************************/

package net.jxta.impl.membership.pse;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyStore;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchProviderException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.id.ID;
import net.jxta.peergroup.PeerGroup;

import net.jxta.impl.cm.Cm;
import net.jxta.impl.peergroup.StdPeerGroup;

/**
 *  Manages a Keystore located within the JXTA CM.
 **/
public class CMKeyStoreManager implements KeyStoreManager {
    
    /**
     *  Log4J Logger
     **/
    private final static transient Logger LOG = Logger.getLogger( CMKeyStoreManager.class.getName() );
    
    private final static String DEFAULT_KEYSTORE_TYPE = "jks";
    
    /**
     *  The keystore type
     **/
    private final String keystore_type;
    
    /**
     *  The keystore type
     **/
    private final String keystore_provider;
    
    /**
     *  The file where the keystore lives
     **/
    private final Cm keystore_cm;
    
    /**
     *  The file where the keystore lives
     **/
    private final ID keystore_location;
    
    /**
     *  Default constructor.
     **/
    public CMKeyStoreManager( String type, String provider, PeerGroup group, ID location  ) throws NoSuchProviderException, KeyStoreException {
        
        if( null == type ) {
            type = DEFAULT_KEYSTORE_TYPE;
            provider = null;
        }
        
        // special case for forcing bc provider for jdk < 1.5 since jdk 1.4.x
        // jsse pkcs12 is readonly.
        if ( "pkcs12".equalsIgnoreCase( type ) ) {
            if( "BC".equals( provider ) ) {
                provider = null;
            }
            
            boolean hasJDK15 = System.getProperty( "java.specification.version", "0.0" ).compareTo( "1.5" ) >= 0;
            
            if( hasJDK15 ) {
                provider = null;
            } else {
                provider = "BC";
            }
        }
        
        keystore_type = type;
        
        keystore_provider = provider;
        
        keystore_cm = ((StdPeerGroup)group).getCacheManager();
        
        keystore_location = location;
        
        // check if we can get an instance.
        if( null == keystore_provider ) {
            KeyStore.getInstance( keystore_type );
        } else {
            KeyStore.getInstance( keystore_type, keystore_provider );
        }
        
        if ( LOG.isEnabledFor(Level.INFO) ) {
            LOG.info( "pse location = " + keystore_location + " in " + keystore_cm );
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean isInitialized() {
        return isInitialized( null );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean isInitialized(char[] store_password ) {
        try {
            KeyStore store;
            if( null == keystore_provider ) {
                store = KeyStore.getInstance( keystore_type );
            } else {
                store = KeyStore.getInstance( keystore_type, keystore_provider );
            }
            
            InputStream is = keystore_cm.getInputStream( "Raw", keystore_location.toString() );
            
            if( null == is ) {
                return false;
            }
            
            store.load( is, store_password );
            
            return true;
        } catch( Exception failed ) {
            return false;
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void createKeyStore( char[] store_password ) throws KeyStoreException, IOException {
        try {
            KeyStore store;
            if( null == keystore_provider ) {
                store = KeyStore.getInstance( keystore_type );
            } else {
                store = KeyStore.getInstance( keystore_type, keystore_provider );
            }
            
            store.load( null, store_password );
            
            saveKeyStore( store, store_password );
        } catch( NoSuchProviderException failed ) {
            KeyStoreException failure = new KeyStoreException( "NoSuchProviderException during keystore processing" );
            failure.initCause( failed );
            throw failure;
        }  catch( NoSuchAlgorithmException failed ) {
            KeyStoreException failure = new KeyStoreException( "NoSuchAlgorithmException during keystore processing" );
            failure.initCause( failed );
            throw failure;
        } catch( CertificateException failed ) {
            KeyStoreException failure = new KeyStoreException( "CertificateException during keystore processing" );
            failure.initCause( failed );
            throw failure;
        }
    }
    
    /**
     *  Return the keystore instance we are using. i
     **/
    public KeyStore loadKeyStore(char[] password) throws KeyStoreException, IOException  {
        
        if ( LOG.isEnabledFor(Level.DEBUG) ) {
            LOG.debug( "Loading (" + keystore_type + "," + keystore_provider +  ") store from " + keystore_location );
        }
        
        try {
            KeyStore store;
            if( null == keystore_provider ) {
                store = KeyStore.getInstance( keystore_type );
            } else {
                store = KeyStore.getInstance( keystore_type, keystore_provider );
            }
            
            InputStream is = keystore_cm.getInputStream( "Raw", keystore_location.toString() );
            
            store.load( is, password );
            
            return store;
        } catch( NoSuchAlgorithmException failed ) {
            KeyStoreException failure = new KeyStoreException( "NoSuchAlgorithmException during keystore processing" );
            failure.initCause( failed );
            throw failure;
        } catch( CertificateException failed ) {
            KeyStoreException failure = new KeyStoreException( "CertificateException during keystore processing" );
            failure.initCause( failed );
            throw failure;
        } catch( NoSuchProviderException failed ) {
            KeyStoreException failure = new KeyStoreException( "NoSuchProviderException during keystore processing" );
            failure.initCause( failed );
            throw failure;
        }
    }
    
    /**
     *  Return the keystore instance we are using. includes compatibility code
     *  to force using Bouncy Castle for < JDK 1.5. The JCE 1.4.X PCKS#12
     *  keystore is read only.
     **/
    public void saveKeyStore(KeyStore store, char[] password) throws IOException, KeyStoreException  {
        
        if ( LOG.isEnabledFor(Level.DEBUG) ) {
            LOG.debug( "Writing " + store + " to " + keystore_location );
        }
        
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            
            store.store( bos, password );
            bos.close();
            
            keystore_cm.save( "Raw", keystore_location.toString(), bos.toByteArray(), Long.MAX_VALUE, 0 );
        } catch( NoSuchAlgorithmException failed ) {
            KeyStoreException failure = new KeyStoreException( "NoSuchAlgorithmException during keystore processing" );
            failure.initCause( failed );
            throw failure;
        } catch( CertificateException failed ) {
            KeyStoreException failure = new KeyStoreException( "CertificateException during keystore processing" );
            failure.initCause( failed );
            throw failure;
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void eraseKeyStore() throws IOException {
        
        keystore_cm.remove( "Raw", keystore_location.toString() );
    }
}
