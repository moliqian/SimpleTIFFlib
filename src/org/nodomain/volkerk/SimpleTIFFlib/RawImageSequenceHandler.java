/*
 * Copyright © 2013 Volker Knollmann
 * 
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file or visit
 * http://www.wtfpl.net/ for more details.
 * 
 * This program comes without any warranty. Use it at your own risk or
 * don't use it at all.
 */
package org.nodomain.volkerk.SimpleTIFFlib;

import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.nodomain.volkerk.LoggingLib.LoggingClass;

/**
 * Handler class for the RAW files generated by Magic Lantern.
 * The RAW files contain pure data blocks of 14-bit little endian CFA data (one
 * per frame) plus a little info block at the file end
 */
public class RawImageSequenceHandler extends LoggingClass{
    
    protected static final int FOOTER_OFFSET_FROM_FILE_END = 192;
    protected static final int FOOTER_MAGIC_OFFSET = 0;
    protected static final int FOOTER_MAGIC_LENGTH = 4;
    protected static final String MAGIC = "RAWM";
    
    protected static final int FOOTER_WIDTH_OFFSET = 4;
    protected static final int FOOTER_HEIGHT_OFFSET = 6;
    
    protected static final int FOOTER_FRAME_SIZE_OFFSET = 8;
    
    protected static final int FOOTER_FRAME_COUNT_OFFSET = 12;
    
    protected static final int FOOTER_FRAME_SKIP_OFFSET = 16;
    protected static final int FOOTER_FPSx1000_OFFSET = 20;
    
    protected static final int FOOTER_RAW_INFO_API_VERSION_OFFSET = 32;
    //protected static final int FOOTER_RAW_INFO_BUFPTR_OFFSET = 40;
    protected static final int FOOTER_RAW_INFO_HEIGHT_OFFSET = 40;
    protected static final int FOOTER_RAW_INFO_WIDTH_OFFSET = 44;
    protected static final int FOOTER_RAW_INFO_PITCH_OFFSET = 48;
    protected static final int FOOTER_RAW_INFO_FRAMESIZE_OFFSET = 52;
    protected static final int FOOTER_RAW_INFO_BPP_OFFSET = 56;
    protected static final int FOOTER_RAW_INFO_BLACKLEVEL_OFFSET = 60;
    protected static final int FOOTER_RAW_INFO_WHITELEVEL_OFFSET = 64;
    protected static final int FOOTER_RAW_INFO_CROPFIELD_OFFSET = 68;
    protected static final int FOOTER_RAW_INFO_ACTIVEAREA_OFFSET = 84;
    protected static final int FOOTER_RAW_INFO_DYNAMICRANGE_OFFSET = 84+26*4;
    
    /**
     * The Path for the RAW input file
     */
    protected Path inFilePath;
    
    /**
     * A RandomAccessFile object for accessing the RAW file contents
     */
    protected RandomAccessFile fData;
    
    public RawImageSequenceHandler(String fname)
    {
        this(Paths.get(fname));
    }
    
    public RawImageSequenceHandler(Path fPath)
    {
        dbg("Constructor called with Path arg ", fPath);
        inFilePath = fPath;

        preLog(LVL_DEBUG, "Trying to open RandomAccessFile for ", fPath);
        try
        {
            fData = new RandomAccessFile(fPath.toString(), "r");
        }
        catch (Exception e)
        {
            resultLog(LOG_FAIL);
            return;
        }
        
        if (fData == null)
        {
            resultLog(LOG_FAIL);
            return;
        }
        
        // check the magic bytes in the footer
        if (!(getMagic().equals(MAGIC)))
        {
            failed("Wrong file type; not a RAW file");
            throw new IllegalArgumentException(fPath.toString() + " is not a valid RAW file");
        }
        
        // make sure that this is 14 bpp, because we can't handle anything else
        if (getRawInfo_BitsPerPixel() != 14)
        {
            failed("Wrong number of bits per pixel (need 14, got ", getRawInfo_BitsPerPixel(), ")");
            throw new IllegalArgumentException(fPath.toString() + " is not a valid RAW file");
        }
        
        resultLog(LOG_OK);
    }
    
    protected void seekInFooter(int offset)
    {
        try
        {
            fData.seek(fData.length() - FOOTER_OFFSET_FROM_FILE_END + offset);
        }
        catch (Exception e)
        {
            failed("seekFromEnd: this shouldn't happen: ", e.getMessage());
            System.exit(42);
        }
    }
    
    public String getMagic()
    {
        seekInFooter(FOOTER_OFFSET_FROM_FILE_END + FOOTER_MAGIC_OFFSET);
        
        byte[] buf = new byte[FOOTER_MAGIC_LENGTH];
        
        for (int i = 0; i < FOOTER_MAGIC_LENGTH; i++)
        {
            seekInFooter(FOOTER_MAGIC_OFFSET + i);
            try
            {
                buf[i] = fData.readByte();
            }
            catch (Exception e)
            {
                return null;
            }
            
        }
        
        try
        {
            return new String(buf, "US-ASCII");
        }
        catch (Exception e) {}
        
        // we should never reach this point
        return null;
    }
    
    public int getWidth()
    {
        seekInFooter(FOOTER_WIDTH_OFFSET);
        return getUint16();
    }
    
    public int getHeight()
    {
        seekInFooter(FOOTER_HEIGHT_OFFSET);
        return getUint16();
    }
    
    public long getFrameSize()
    {
        seekInFooter(FOOTER_FRAME_SIZE_OFFSET);
        return getUint32();
    }
    
    public long getFrameCount()
    {
        seekInFooter(FOOTER_FRAME_COUNT_OFFSET);
        return getUint32();
    }
    
    public long getFrameSkip()
    {
        seekInFooter(FOOTER_FRAME_SKIP_OFFSET);
        return getUint32();        
    }
    
    public long getFrameRate1000()
    {
        seekInFooter(FOOTER_FPSx1000_OFFSET);
        return getUint32();        
    }
    
    public double getFrameRate()
    {
        return getFrameRate1000() / 1000.0;
    }
    
    public long getRawInfo_APIVersion()
    {
        seekInFooter(FOOTER_RAW_INFO_API_VERSION_OFFSET);
        return getUint32();        
    }
    
//    public long getRawInfo_BufferPointer()
//    {
//        seekInFooter(FOOTER_RAW_INFO_BUFPTR_OFFSET);
//        return getUint32();        
//    }
    
    public long getRawInfo_Height()
    {
        seekInFooter(FOOTER_RAW_INFO_HEIGHT_OFFSET);
        return getUint32();        
    }
    
    public long getRawInfo_Width()
    {
        seekInFooter(FOOTER_RAW_INFO_WIDTH_OFFSET);
        return getUint32();        
    }
    
    public long getRawInfo_Pitch()
    {
        seekInFooter(FOOTER_RAW_INFO_PITCH_OFFSET);
        return getUint32();        
    }
    
    public long getRawInfo_FrameSize()
    {
        seekInFooter(FOOTER_RAW_INFO_FRAMESIZE_OFFSET);
        return getUint32();        
    }
    
    public long getRawInfo_BitsPerPixel()
    {
        seekInFooter(FOOTER_RAW_INFO_BPP_OFFSET);
        return getUint32();        
    }
    
    public long getRawInfo_BlackLevel()
    {
        seekInFooter(FOOTER_RAW_INFO_BLACKLEVEL_OFFSET);
        return getUint32();        
    }
    
    public long getRawInfo_WhiteLevel()
    {
        seekInFooter(FOOTER_RAW_INFO_WHITELEVEL_OFFSET);
        return getUint32();        
    }
    
    public long[] getRawInfo_Crop()
    {
        seekInFooter(FOOTER_RAW_INFO_CROPFIELD_OFFSET);
        long[] result = new long[4];
        
        for (int i=0; i<4; i++) result[i] = getUint32();
        
        return result;
    }
    
    public long[] getRawInfo_ActiveArea()
    {
        seekInFooter(FOOTER_RAW_INFO_ACTIVEAREA_OFFSET);
        long[] result = new long[4];
        
        for (int i=0; i<4; i++) result[i] = getUint32();
        
        return result;
    }
    
    public long getRawInfo_DynamicRange100()
    {
        seekInFooter(FOOTER_RAW_INFO_DYNAMICRANGE_OFFSET);
        return getUint32();        
    }
    
    public double getRawInfo_DynamicRange()
    {
        return getRawInfo_DynamicRange100() / 100.0;
    }
    
    protected int getUint16()
    {
        try
        {
            int a = fData.readUnsignedByte();
            int b = fData.readUnsignedByte();
            return ((b & 0xff) << 8) | (a & 0xff);
        }
        catch (Exception e)
        {
            System.err.println("Aaaaaaaaaargh.......... getUint16 in RAW");
            System.exit(42);
        }
        
        // we should never reach this point
        return -1;
    }
    
    protected long getUint32()
    {
        try
        {
            int a = fData.readUnsignedByte();
            int b = fData.readUnsignedByte();
            int c = fData.readUnsignedByte();
            int d = fData.readUnsignedByte();
            return (((d & 0xff) << 24) | ((c & 0xff) << 16) | ((b & 0xff) << 8) | (a & 0xff));
        }
        catch (Exception e)
        {
            System.err.println("Aaaaaaaaaargh.......... getUint32 in RAW");
            System.exit(42);
        }
        
        // we should never reach this point
        return -1;
    }
    
    public void dumpInfo()
    {
        System.err.println("----------- Raw File Footer -----------");
        System.err.println("Magic tokens: " + getMagic());
        System.err.println("Wdith: " + getWidth());
        System.err.println("Height: " + getHeight());
        System.err.println("Bytes per frame: " + getFrameSize());
        System.err.println("Frames in file: " + getFrameCount());
        System.err.println("Frame skip: " + getFrameSkip());
        System.err.println("Frame rate: " + getFrameRate());
        System.err.println();
        System.err.println("----------- Raw Info Struct -----------");
        System.err.println("API version: " + getRawInfo_APIVersion());
        //System.err.println("Buffer pointer: " + getRawInfo_BufferPointer());
        System.err.println("Width: " + getRawInfo_Width());
        System.err.println("Height: " + getRawInfo_Height());
        System.err.println("Pitch: " + getRawInfo_Pitch());
        System.err.println("Frame size: " + getRawInfo_FrameSize());
        System.err.println("Bits per Pixel: " + getRawInfo_BitsPerPixel());
        System.err.println("Black level: " + getRawInfo_BlackLevel());
        System.err.println("White level: " + getRawInfo_WhiteLevel());
        System.err.println("Crop x, y, w, h: " + arrayToString(getRawInfo_Crop()));
        System.err.println("Active area x1, y1, x2, y2: " + arrayToString(getRawInfo_ActiveArea()));
        System.err.println("Dynamic Range (EV): " + getRawInfo_DynamicRange());
    }

    /**
     * Converts an array of longs to a string with comma-separated values for beautiful logging
     * 
     * @param longArray the array with the longs for conversion
     * 
     * @return a string with comma-separated longs
     */
    protected String arrayToString(long[] longArray)
    {
        String result = "";
        for (long i : longArray)
        {
            result += i + ", ";
        }
        return result.substring(0, result.length()-2);
    }
}
