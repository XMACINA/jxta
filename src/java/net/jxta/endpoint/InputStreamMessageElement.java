/************************************************************************
 *
 * $Id: InputStreamMessageElement.java,v 1.1 2007/01/16 11:01:26 thomas Exp $
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
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.zip.CRC32;         // used in hashCode
import java.util.zip.Checksum;

import java.io.IOException;

import net.jxta.document.MimeMediaType;

/**
 * A Message Element using {@link java.io.InputStream} as the source for the
 * element data. This implementation copies all of the data from the stream at
 * the time of creation.
 **/
public class InputStreamMessageElement extends MessageElement {
    
    /**
     *  The bytes of this element.
     **/
    protected List databytes = null;
    
    /**
     *  The length of the data.
     **/
    protected long length = 0;
    
    /**
     *  Cached Hash Code
     **/
    protected transient int cachedHashCode = 0;
    
    /**
     *  Create a new MessageElement. This constructor copies the data as needed
     *  and closes the stream upon completion. If the stream does not support
     *  the "mark" operation then the data is copied immediately and the stream
     *  will be closed.
     *
     * @param name Name of the MessageElement. May be the empty string ("") if
     * the MessageElement is not named.
     * @param type Type of the MessageElement. null is the same as specifying
     * the type "Application/Octet-stream".
     * @param in the stream containing the body of the MessageElement. The
     * stream will be closed by the MessageElement.
     * @param sig optional message digest/digital signature elemnent or null if
     * no signature is desired.
     * @throws IOException If there is a problem reading from the source stream.
     **/
    public InputStreamMessageElement(String name, MimeMediaType type, InputStream in, MessageElement sig) throws IOException {
        this( name, type, in, Long.MAX_VALUE, sig );
    }
    
    /**
     * Create a new Message Element.
     *
     * @param name Name of the MessageElement. May be the empty string ("") if
     * the MessageElement is not named.
     * @param type Type of the MessageElement. null is the same as specifying
     * the type "Application/Octet-stream".
     * @param in the stream containing the body of the MessageElement.
     * The stream will <b>NOT</b> be closed unless EOF is unexpectedly reached.
     * @param len The size of the Element will be limited to len bytes
     *    from the stream. If you are using the stream interface and know
     *    the size of the stream, specifying it here improves performance
     * and space effciency a lot. The stream must contain at least
     * <code>len</code> bytes.
     * @param sig optional message digest/digital signature elemnent or null if
     * no signature is desired.
     * @throws IOException if there is a problem reading from the source stream
     **/
    public InputStreamMessageElement( String name, MimeMediaType type, InputStream in, long len, MessageElement sig ) throws IOException {
        super( name, type, sig );
        
        if( (len < 0) ) {
            throw new IllegalArgumentException( "len must be >= 0" );
        }
        
        // copy the data from the stream
        databytes = CopyToDataBytes( in, len );
        
        // calculate the length
        length = 0;
        Iterator eachBuffer = databytes.iterator();
        while(  eachBuffer.hasNext() ) {
            byte [] aBuffer = (byte []) eachBuffer.next();
            
            length += aBuffer.length;
        }
        
        // fail if the length is not as promised.
        if( (len != Long.MAX_VALUE) && (len != length)) {
            throw new IllegalArgumentException( "Stream was shorter than promised length." );
        }
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
            
            if( target instanceof InputStreamMessageElement ) {
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
    public synchronized int hashCode() {
        if ( 0 == cachedHashCode ) {
            Checksum crc = new CRC32();
            
            Iterator eachBuffer = databytes.iterator();
            while( eachBuffer.hasNext() ) {
                byte [] aBuffer = (byte []) eachBuffer.next();
                
                crc.update( aBuffer, 0, aBuffer.length );
            }
            
            int result = super.hashCode() +
            (int) crc.getValue() * 6037;   // a prime
            cachedHashCode = 0 != result ? result : 1;
        }
        
        return cachedHashCode;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public long getByteLength() {
        return length;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public InputStream getStream() throws IOException {
        List buffers = new ArrayList();
        
        Iterator eachBuffer = databytes.iterator();
        while( eachBuffer.hasNext() ) {
            byte [] aBuffer = (byte []) eachBuffer.next();
            buffers.add( new ByteArrayInputStream(aBuffer) );
        }
        
        return new SequenceInputStream( Collections.enumeration(buffers) );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void sendToStream(OutputStream sendTo) throws IOException {
        
        Iterator eachBuffer = databytes.iterator();
        while( eachBuffer.hasNext() ) {
            byte [] aBuffer = (byte []) eachBuffer.next();
            sendTo.write( aBuffer );
        }
    }
    
    /**
     *  Copy data from a stream with best possible effciency. Unfortunately,
     *  this still results in a lot of copying since we have often have no
     * fore-knowledge of the length of the stream.
     *
     * @return an array of byte arrays containing the data.
     * @param in the stream to copy from
     * @param limit the maximum number of bytes to copy from the stream.
     * Long.LONG_MAX will read until EOF.
     * @throws IOException if there is a problem reading from the stream.
     **/
    protected List CopyToDataBytes( InputStream in, long limit ) throws IOException {
        final long INITIAL_INTERMEDIATE_BUFFERSIZE = 6;
        final long MAXIMUM_INTERMEDIATE_BUFFERSIZE = 18;
        
        List buffs = new ArrayList();
        boolean atEOF = false;
        long read = 0;
        long currentIntermediateBufferSize = INITIAL_INTERMEDIATE_BUFFERSIZE;
        
        // build a list of buffers containing all the element data.
        do {
            long readRequest = (limit - read);
            if( Long.MAX_VALUE == limit ) {
                readRequest = Math.min( readRequest, (1L << currentIntermediateBufferSize) );
            }
            readRequest = Math.min( readRequest, Integer.MAX_VALUE ); // limited by size of arrays which are Integer indexed.
            
            byte [] nextBuffer = new byte [(int) readRequest];
            int offsetInThisBuffer = 0;
            
            // fully read the buffer if we can.
            do {
                int readLength = in.read( nextBuffer, offsetInThisBuffer, nextBuffer.length - offsetInThisBuffer );
                
                if( readLength == -1) {
                    atEOF = true;
                    break;
                }
                
                offsetInThisBuffer += readLength;
            } while( offsetInThisBuffer < nextBuffer.length );
            
            // handle the final buffer.
            if( atEOF ) {
                byte [] anotherBuffer = new byte[offsetInThisBuffer];
                System.arraycopy( nextBuffer, 0, anotherBuffer, 0, offsetInThisBuffer );
                nextBuffer = anotherBuffer;
            }
            
            read += nextBuffer.length;
            buffs.add( nextBuffer );
            
            if( currentIntermediateBufferSize < MAXIMUM_INTERMEDIATE_BUFFERSIZE ) {
                currentIntermediateBufferSize++;
            }
        } while ( !atEOF && (read < limit) );
        
        // we are done, close if we are at EOF.
        if( atEOF ) {
            in.close();
            in = null;
        }
        
        return buffs;
    }
}
