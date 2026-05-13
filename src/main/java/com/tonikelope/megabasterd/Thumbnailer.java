package com.tonikelope.megabasterd;

import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.Utils;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class Thumbnailer {

    public Thumbnailer() {
    }

    public static final int IMAGE_THUMB_SIZE = 250;

    public static final float SECONDS_BETWEEN_FRAMES_PERC = 0.03f; //Take frame video at 3% position

    /**
     * The number of nano-seconds between frames.
     */
    private long nano_seconds_between_frames;

    /**
     * Time of last frame write.
     */
    private long mLastPtsWrite = Global.NO_PTS;

    private int conta_frames = 0;

    /**
     * Write the video frame out to a PNG file every once and a while. The files
     * are written out to the system's temporary directory.
     *
     * @param picture the video frame which contains the time stamp.
     * @param image the buffered image to write out
     */
    private String processFrame(IVideoPicture picture, BufferedImage image) {
        try {
            // if uninitialized, backdate mLastPtsWrite so we get the very
            // first frame

            if (mLastPtsWrite == Global.NO_PTS) {
                mLastPtsWrite = picture.getPts() - nano_seconds_between_frames;
            }

            // if it's time to write the next frame
            if (picture.getPts() - mLastPtsWrite >= nano_seconds_between_frames) {
                // Make a temorary file name

                if (conta_frames == 1) {

                    File file = File.createTempFile("megabasterd_thumbnail_" + MiscTools.genID(20), ".jpg");

                    file.deleteOnExit();

                    // write out JPG
                    ImageIO.write(image, "jpg", file);

                    // indicate file written
                    //double seconds = ((double) picture.getPts()) / Global.DEFAULT_PTS_PER_SECOND;
                    //System.out.printf("at elapsed time of %6.3f seconds wrote: %s\n", seconds, file);
                    conta_frames++;

                    return file.getAbsolutePath();
                }

                conta_frames++;

                // update last write time
                mLastPtsWrite += nano_seconds_between_frames;

                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String createThumbnail(String filename) {
        try {
            if (MiscTools.isVideoFile(filename)) {

                return createVideoThumbnail(filename);

            } else if (MiscTools.isImageFile(filename)) {

                return createImageThumbnail(filename);

            }
        } catch (Exception ex) {
        }

        return null;
    }

    private String createImageThumbnail(String filename) {

        try {

            BufferedImage imagen_original = ImageIO.read(new File(filename));

            if (imagen_original.getHeight() <= IMAGE_THUMB_SIZE) {
                return filename;
            }

            int h = IMAGE_THUMB_SIZE;

            int w = Math.round((((float) imagen_original.getWidth()) * h) / imagen_original.getHeight());

            BufferedImage newImage = new BufferedImage(w, h, imagen_original.getType());

            Graphics2D g = newImage.createGraphics();

            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            g.drawImage(imagen_original, 0, 0, w, h, null);

            g.dispose();

            File file = File.createTempFile("megabasterd_thumbnail_" + MiscTools.genID(20), ".png");

            file.deleteOnExit();

            ImageIO.write(newImage, "png", file);

            return file.getAbsolutePath();

        } catch (Exception ex) {
            Logger.getLogger(Thumbnailer.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * Takes a media container (file) as the first argument, opens it, reads
     * through the file and captures video frames periodically as specified by
     * SECONDS_BETWEEN_FRAMES. The frames are written as PNG files into the
     * system's temporary directory.
     *
     * @param args must contain one string which represents a filename
     */
    @SuppressWarnings("deprecation")
    private String createVideoThumbnail(String filename) {

        // make sure that we can actually convert video pixel formats
        if (!IVideoResampler.isSupported(
                IVideoResampler.Feature.FEATURE_COLORSPACECONVERSION)) {
            throw new RuntimeException(
                    "you must install the GPL version of Xuggler (with IVideoResampler"
                    + " support) for this demo to work");
        }

        // create a Xuggler container object
        IContainer container = IContainer.make();
        IStreamCoder videoCoder = null;
        IVideoResampler resampler = null;
        IPacket packet = null;

        try {

            // open up the container
            if (container.open(filename, IContainer.Type.READ, null) < 0) {
                throw new IllegalArgumentException("could not open file: " + filename);
            }

            nano_seconds_between_frames = (long) (Global.DEFAULT_PTS_PER_SECOND * Math.round((float) SECONDS_BETWEEN_FRAMES_PERC * container.getDuration() / 1000000));

            // query how many streams the call to open found
            int numStreams = container.getNumStreams();

            // and iterate through the streams to find the first video stream
            int videoStreamId = -1;
            for (int i = 0; i < numStreams; i++) {

                IStream stream = container.getStream(i);

                IStreamCoder coder = stream.getStreamCoder();

                if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                    videoStreamId = i;
                    videoCoder = coder;
                    break;
                }
            }

            if (videoStreamId == -1) {
                throw new RuntimeException("could not find video stream in container: " + filename);
            }

            if (videoCoder.open() < 0) {
                throw new RuntimeException(
                        "could not open video decoder for container: " + filename);
            }

            if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {

                resampler = IVideoResampler.make(
                        videoCoder.getWidth(), videoCoder.getHeight(), IPixelFormat.Type.BGR24,
                        videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());
                if (resampler == null) {
                    throw new RuntimeException(
                            "could not create color space resampler for: " + filename);
                }
            }

            packet = IPacket.make();

            String frame_file = null;

            while (container.readNextPacket(packet) >= 0 && conta_frames < 2) {

                if (packet.getStreamIndex() == videoStreamId) {

                    IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(),
                            videoCoder.getWidth(), videoCoder.getHeight());

                    IVideoPicture newPic = null;

                    try {

                        int offset = 0;

                        while (offset < packet.getSize()) {

                            int bytesDecoded = videoCoder.decodeVideo(picture, packet, offset);
                            if (bytesDecoded < 0) {
                                throw new RuntimeException("got error decoding video in: " + filename);
                            }
                            offset += bytesDecoded;

                            if (picture.isComplete()) {
                                newPic = picture;

                                if (resampler != null) {
                                    newPic = IVideoPicture.make(
                                            resampler.getOutputPixelFormat(), picture.getWidth(),
                                            picture.getHeight());
                                    if (resampler.resample(newPic, picture) < 0) {
                                        throw new RuntimeException(
                                                "could not resample video from: " + filename);
                                    }
                                }

                                if (newPic.getPixelType() != IPixelFormat.Type.BGR24) {
                                    throw new RuntimeException(
                                            "could not decode video as BGR 24 bit data in: " + filename);
                                }

                                BufferedImage javaImage = Utils.videoPictureToImage(newPic);

                                frame_file = processFrame(newPic, javaImage);
                            }
                        }

                    } finally {
                        if (newPic != null && newPic != picture) {
                            newPic.delete();
                        }
                        picture.delete();
                    }
                }

            }

            return frame_file;

        } finally {

            if (packet != null) {
                try {
                    packet.delete();
                } catch (Throwable ignore) {
                }
            }

            if (resampler != null) {
                try {
                    resampler.delete();
                } catch (Throwable ignore) {
                }
            }

            if (videoCoder != null) {
                try {
                    videoCoder.close();
                } catch (Throwable ignore) {
                }
            }

            if (container != null) {
                try {
                    container.close();
                } catch (Throwable ignore) {
                }
            }
        }
    }
}
