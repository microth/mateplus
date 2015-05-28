package se.lth.cs.srl.http.whatswrongglue;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import com.googlecode.whatswrong.NLPCanvasRenderer;
import com.googlecode.whatswrong.NLPInstance;

public class Export {
    
    private static BufferedImage getImage(NLPCanvasRenderer renderer,NLPInstance instance,double scaleFactor){
    	BufferedImage image=new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
    	Graphics2D graphics=image.createGraphics();
    	Dimension d=renderer.render(instance, graphics);
    	
    	if(scaleFactor!=1)
    		d=new Dimension((int) (d.width*scaleFactor),(int) (d.height*scaleFactor));
    	
    	image=new BufferedImage((int) d.getWidth(),(int) d.getHeight(),BufferedImage.TYPE_4BYTE_ABGR);
    	graphics=image.createGraphics();
    	
    	if(scaleFactor!=1)
    		graphics.scale(scaleFactor, scaleFactor);
    	
    	renderer.render(instance, graphics);
    	return image;
    }
    
    public static void exportToJPG(File file,NLPCanvasRenderer renderer,NLPInstance instance,double scaleFactor) throws FileNotFoundException, IOException{
    	BufferedOutputStream os=new BufferedOutputStream(new FileOutputStream(file));
    	exportToJPG(os,renderer,instance,scaleFactor);
    	os.close();
    }
    
    /**
     * Export to JPG. For some reason the background comes out pink, I have no idea why.
     * Note that this method /does not/ close the outputstream after writing, this is up to the caller.
     * 
     * @param os The stream to write to
     * @param renderer The renderer to use
     * @param instance The instance to render
     * @param scaleFactor The scaling factor. Set to 1 for default size.
     * @throws IOException 
     */
    public static void exportToJPG(OutputStream os,NLPCanvasRenderer renderer,NLPInstance instance,double scaleFactor) throws IOException{
    	BufferedImage image=getImage(renderer,instance,scaleFactor);
    	ImageIO.write(image,"JPG",os);
    }
    
    public static void exportToPNG(File file,NLPCanvasRenderer renderer,NLPInstance instance,double scaleFactor) throws FileNotFoundException, IOException{
    	BufferedOutputStream os=new BufferedOutputStream(new FileOutputStream(file));
    	exportToPNG(os,renderer,instance,scaleFactor);
    	os.close();
    }
    
   /**
    * Export to PNG.
    * Note that this method /does not/ close the outputstream after writing, this is up to the caller.
    *  
    * @param os The stream to write to
    * @param renderer The renderer to use
    * @param instance The instance to render
    * @param scaleFactor The scaling factor. Set to 1 for default size.
    * @throws IOException 
    */
    public static void exportToPNG(OutputStream os,NLPCanvasRenderer renderer,NLPInstance instance,double scaleFactor) throws IOException{
    	BufferedImage image=getImage(renderer,instance,scaleFactor);
    	ImageIO.write(image,"PNG",os);	
    }
	
}
