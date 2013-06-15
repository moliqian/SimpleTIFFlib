/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nodomain.volkerk.SimpleTIFFlib;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;

/**
 *
 * @author volker
 */
public class TIFFhandler {
    protected static final int MIN_FILE_SIZE = 20;
    
    /**
     * The Path for the TIFF input file
     */
    protected Path inFilePath;
    
    /**
     * A copy of the complete input file
     */
    protected FlexByteArray fData = null;
    
    /**
     * a list of all image file directories in the file
     */
    ArrayList<ImageFileDirectory> ifdList;
    
    /**
     * Constructor. Takes the input file name and reads all bytes into an array
     * 
     * @param fName the path to the input file
     * @throws IOException 
     */
    public TIFFhandler(String fName) throws IOException
    {
        this(Paths.get(fName));
    }
    
    /**
     * Constructor. Takes the input file name and reads all bytes into an array
     * 
     * @param fPath Path-object for the input file
     * @throws IOException 
     */
    public TIFFhandler(Path fPath) throws IOException
    {
        inFilePath = fPath;
        
        fData = new FlexByteArray(Files.readAllBytes(inFilePath));
        
        // check some parameters
        if (fData.length() < MIN_FILE_SIZE)
        {
            throw new IllegalArgumentException(fPath.toString() + " is not a TIFF file");
        }
        
        // check header to see if we have a TIFF
        int firstTwoBytes = fData.getUint16(0);
        if (firstTwoBytes == 0x4949)
        {
            //System.err.println("Found little endian tag in header.");
            fData.setSwap(false);
        }
        else if (firstTwoBytes == 0x4d4d)
        {
            //System.err.println("Found big endian tag in header. Swapping byte order!");
            fData.setSwap(true);
        }
        else throw new IllegalArgumentException("First two bytes in file invalid!");
        
        if (fData.getUint16(2) != 42) throw new IllegalArgumentException("Missing 42-tag in header!");
        
        // if we've reached this point, we can be pretty sure to have a valid TIFF file
        int firstDirOffset = (int) fData.getUint32(4);
        //System.err.println("First IFD at offset " + firstDirOffset);
        initDirectories(firstDirOffset);
    }
    
    protected void initDirectories(int firstDirectoryOffset)
    {
        ifdList = new ArrayList<>();
        
        int nextOffset = firstDirectoryOffset;
        while (nextOffset != 0)
        {
            ImageFileDirectory d = new ImageFileDirectory(fData, nextOffset);
            ifdList.add(d);
            
            nextOffset = (int) d.getNextDirectoryOffset();
            
            // Some debug data
            //d.dumpInfo();
            
            // Sub-IFD?
            if (d.hasTag(TIFF_TAG.SUB_IFDs))
            {
                IFD_Entry e = d.getEntry(TIFF_TAG.SUB_IFDs);
                if (e.getNumVal() == 1)
                {
                    ImageFileDirectory subDir = new ImageFileDirectory(fData, (int)e.getLong(), d);
                    //subDir.dumpInfo();
                    ifdList.add(subDir);
                }
            }
        }
    }
    
    public void dumpRawToPng(String destFileName)
    {
        ImageFileDirectory ifd = getFirstIFDwithCFA();
        if (ifd != null) ifd.CFA_raw2png(destFileName, false);
        else System.err.println("dumpRawToPng: no RAW data found in image!");
    }
    
    public ImageFileDirectory getFirstIFDwithCFA()
    {
        for (ImageFileDirectory ifd : ifdList)
        {
            if (ifd.photometricInterpretation() != TIFF_TAG.PHOTO_INTERPRETATION_CFA) continue;
            return ifd;
        }
        return null;
    }
    
    public void primitiveDemosaic(String destFileName)
    {
        ImageFileDirectory ifd = getFirstIFDwithCFA();
        if (ifd != null) ifd.CFA_primitiveDemosaic(destFileName);
        else System.err.println("dumpRawToPng: no RAW data found in image!");
    }
    
    public void saveAs(Path dstPath)
    {
        saveAs(dstPath.toString());
    }
    
    public void saveAs(String fname)
    {
        fData.dumpToFile(fname);
    }
    
}