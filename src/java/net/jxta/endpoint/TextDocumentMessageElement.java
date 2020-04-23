/************************************************************************
 *
 * $Id: TextDocumentMessageElement.java,v 1.1 2007/01/16 11:01:28 thomas Exp $
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

package net.jxta.endpoint;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.ref.SoftReference;

import java.io.UnsupportedEncodingException;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.MimeMediaType;
import net.jxta.document.TextDocument;

/**
 *  A Message Element using JXTA TextDocument for the element data.
 *
 **/
public class TextDocumentMessageElement extends TextMessageElement {
    
    /**
     *  Log4J Logger
     **/
    private static final Logger LOG = Logger.getLogger( TextDocumentMessageElement.class.getName() );
    
    /**
     *  The data for this element.
     **/
    protected TextDocument doc;
    
    /**
     *  Create a new Message Element from the provided Document.
     *
     *  @param name Name of the Element. May be the empty string ("") or null if
     *    the Element is not named.
     *  @param doc A Document containing the contents of this element.
     *  @param sig optional message digest/digital signature elemnent. If
     *    no signature is to be specified, pass null.
     **/
    public TextDocumentMessageElement(String name, TextDocument doc, MessageElement sig) {
        super( name, doc.getMimeType(), sig );
        
        this.doc = doc;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean equals( Object target ) {
        if( this == target ) {
            return true;
        }
        
        if( target instanceof MessageElement ) {
            if( !super.equals(target) )
                return false;
            
            if ( target instanceof TextMessageElement) {
                // have to do a slow char by char comparison. Still better than the stream since it saves encoding.
                // XXX 20020615 bondolo@jxta.org the performance of this could be much improved.
                
                TextMessageElement likeMe = (TextMessageElement) target;
                
                try {
                    Reader myReader = getReader();
                    Reader itsReader = likeMe.getReader();
                    
                    int mine;
                    int its;
                    do {
                        mine = myReader.read();
                        its = itsReader.read();
                        
                        if( mine != its )
                            return false;       // content didn't match
                        
                    } while( (-1 != mine) && (-1 != its) );
                    
                    return ( (-1 == mine) && (-1 == its) ); // end at the same time?
                } catch( IOException fatal ) {
                    throw new IllegalStateException( "MessageElements could not be compared." + fatal );
                }
            } else {
                // have to do a slow stream comparison.
                // XXX 20020615 bondolo@jxta.org the performance of this could be much improved.
                
                MessageElement likeMe = (MessageElement) target;
                
                try {
                    InputStream myStream = getStream();
                    InputStream itsStream = likeMe.getStream();
                    
                    int mine;
                    int its;
                    do {
                        mine = myStream.read();
                        its = itsStream.read();
                        
                        if( mine != its )
                            return false;       // content didn't match
                        
                    } while( (-1 != mine) && (-1 != its) );
                    
                    return ( (-1 == mine) && (-1 == its) ); // end at the same time?
                } catch( IOException fatal ) {
                    throw new IllegalStateException( "MessageElements could not be compared." + fatal );
                }
            }
        }
        
        return false; // not a new message element
    }
    
    /**
     *  {@inheritDoc}
     **/
    public int hashCode() {
        int result = super.hashCode() * 6037 + // a prime
        toString().hashCode();
        
        return result;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public synchronized String toString() {
        String result = null;
        
        if( null != cachedToString ) {
            result = (String) cachedToString.get();
            
            if (null != result)
                return result;
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)){
            LOG.debug( "creating toString of " + getClass().getName() + "@" + super.hashCode() );
        }
        
        result = doc.toString();
        cachedToString = new SoftReference( result );
        return result;
    }
    
    /**
     * {@inheritDoc}
     **/
    public MimeMediaType getMimeType() {
        return doc.getMimeType();
    }
    
    /**
     * {@inheritDoc}
     **/
    public String getFileExtension() {
        return doc.getFileExtension();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public InputStream getStream() throws IOException {
        byte [] sending = getBytes(false);

        return new ByteArrayInputStream(sending);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void sendToStream( OutputStream sendTo ) throws IOException {

        byte [] sending = getBytes(false);
        
        sendTo.write( sending, 0, sending.length );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Reader getReader() throws IOException {
        
        return doc.getReader( );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void sendToWriter( Writer sendTo ) throws IOException {
        doc.sendToWriter( sendTo );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public byte[] getBytes( boolean copy ) {
        byte [] result = null;
        
        if( null != cachedGetBytes ) {
            result = (byte []) cachedGetBytes.get();
            
            if (null != result)
                if ( copy ) {
                    byte [] theCopy = new byte[ result.length ];
                    
                    System.arraycopy( theCopy, 0, result, 0, result.length );
                } else {
                    return result;
                }
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)){
            LOG.debug( "creating getBytes of " + getClass().getName() + '@' + Integer.toHexString(hashCode())  );
        }
        
        String charset = type.getParameter( "charset" );
        
        if( null == charset )
            result = toString().getBytes( );
        else {
            try {
                result = toString().getBytes( charset );
            } catch( UnsupportedEncodingException caught ) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn( "MessageElement Data could not be generated", caught );
                }
                throw new IllegalStateException( "MessageElement Data could not be generated due to " + caught.getMessage() );
            }
        }
        
        // if this is supposed to be a shared buffer then we can cache it.
        if( !copy ) {
            cachedGetBytes = new SoftReference( result );
        }
        
        return result;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public long getCharLength() {
        return toString().length();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public char[] getChars( boolean copy ) {
        char [] result = null;
        
        if( null != cachedGetChars ) {
            result = (char []) cachedGetChars.get();
            
            if (null != result)
                if ( copy ) {
                    char [] theCopy = new char[ result.length ];
                    
                    System.arraycopy( theCopy, 0, result, 0, result.length );
                } else {
                    return result;
                }
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)){
            LOG.debug( "creating getChars of " + getClass().getName() + '@' + Integer.toHexString(hashCode())  );
        }
        
        String asString = toString();
        
        result = asString.toCharArray();
        
        // if this is supposed to be a shared buffer then we can cache it.
        if( !copy ) {
            cachedGetChars = new SoftReference( result );
        }
        
        return result;
    }
}
