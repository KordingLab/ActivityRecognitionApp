package edu.northwestern.sohrob.activityrecognition.activityrecognition;

/**
 * Created by sohrob on 11/10/14.
 */


import android.util.Log;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeatureExtractor {

    private long _windowSize = -1;  // TODO: must be used in interpolate()
    private int _dimensions = -1;

    private boolean _hasFFT = false;
    private boolean _hasDiff = false;
    private boolean _hasHist = false;
    private boolean _hasCross = false;
    private boolean _hasNormCross = false;

    private FastFourierTransformer _fft = null;

    // bin edges must be in ascending order and equally spaced.
    private double[] _binEdges = new double[]{-3, -2, -1, 0, 1, 2, 3};

    private int _NFFT = 19;

    public static enum Feature {
        ACC_NUM_SAMPLES, ACC_MEAN, ACCX_MEAN, ACCY_MEAN, ACCZ_MEAN, ACC_MEAN_ABS, ACCX_MEAN_ABS, ACCY_MEAN_ABS, ACCZ_MEAN_ABS, ACCX_STD, ACCY_STD, ACCZ_STD, ACCX_SKEW, ACCY_SKEW, ACCZ_SKEW, ACCX_KURT, ACCY_KURT, ACCZ_KURT,
        ACCX_DIFF_MEAN, ACCY_DIFF_MEAN, ACCZ_DIFF_MEAN, ACCX_DIFF_STD, ACCY_DIFF_STD, ACCZ_DIFF_STD, ACCX_DIFF_SKEW, ACCY_DIFF_SKEW, ACCZ_DIFF_SKEW, ACCX_DIFF_KURT, ACCY_DIFF_KURT, ACCZ_DIFF_KURT,
        ACCX_MAX, ACCY_MAX, ACCZ_MAX, ACCX_MIN, ACCY_MIN, ACCZ_MIN, ACCX_MAX_ABS, ACCY_MAX_ABS, ACCZ_MAX_ABS, ACCX_MIN_ABS, ACCY_MIN_ABS, ACCZ_MIN_ABS, ACCX_RMS, ACCY_RMS, ACCZ_RMS,
        ACC_CROSS_XY, ACC_CROSS_YZ, ACC_CROSS_ZX, ACC_CROSS_XY_ABS, ACC_CROSS_YZ_ABS, ACC_CROSS_ZX_ABS, ACC_CROSS_XY_NORM, ACC_CROSS_YZ_NORM, ACC_CROSS_ZX_NORM, ACC_CROSS_XY_NORM_ABS, ACC_CROSS_YZ_NORM_ABS, ACC_CROSS_ZX_NORM_ABS,
        ACCX_FFT1, ACCX_FFT2, ACCX_FFT3, ACCX_FFT4, ACCX_FFT5, ACCX_FFT6, ACCX_FFT7, ACCX_FFT8, ACCX_FFT9, ACCX_FFT10, ACCX_FFT11, ACCX_FFT12, ACCX_FFT13, ACCX_FFT14, ACCX_FFT15, ACCX_FFT16, ACCX_FFT17, ACCX_FFT18, ACCX_FFT19,
        ACCY_FFT1, ACCY_FFT2, ACCY_FFT3, ACCY_FFT4, ACCY_FFT5, ACCY_FFT6, ACCY_FFT7, ACCY_FFT8, ACCY_FFT9, ACCY_FFT10, ACCY_FFT11, ACCY_FFT12, ACCY_FFT13, ACCY_FFT14, ACCY_FFT15, ACCY_FFT16, ACCY_FFT17, ACCY_FFT18, ACCY_FFT19,
        ACCZ_FFT1, ACCZ_FFT2, ACCZ_FFT3, ACCZ_FFT4, ACCZ_FFT5, ACCZ_FFT6, ACCZ_FFT7, ACCZ_FFT8, ACCZ_FFT9, ACCZ_FFT10, ACCZ_FFT11, ACCZ_FFT12, ACCZ_FFT13, ACCZ_FFT14, ACCZ_FFT15, ACCZ_FFT16, ACCZ_FFT17, ACCZ_FFT18, ACCZ_FFT19,
        ACCX_HIST1, ACCX_HIST2, ACCX_HIST3, ACCX_HIST4, ACCX_HIST5, ACCX_HIST6, ACCY_HIST1, ACCY_HIST2, ACCY_HIST3, ACCY_HIST4, ACCY_HIST5, ACCY_HIST6, ACCZ_HIST1, ACCZ_HIST2, ACCZ_HIST3, ACCZ_HIST4, ACCZ_HIST5, ACCZ_HIST6,
        GYR_NUM_SAMPLES, GYR_MEAN, GYRX_MEAN, GYRY_MEAN, GYRZ_MEAN, GYR_MEAN_ABS, GYRX_MEAN_ABS, GYRY_MEAN_ABS, GYRZ_MEAN_ABS, GYRX_STD, GYRY_STD, GYRZ_STD, GYRX_SKEW, GYRY_SKEW, GYRZ_SKEW, GYRX_KURT, GYRY_KURT, GYRZ_KURT,
        GYRX_DIFF_MEAN, GYRY_DIFF_MEAN, GYRZ_DIFF_MEAN, GYRX_DIFF_STD, GYRY_DIFF_STD, GYRZ_DIFF_STD, GYRX_DIFF_SKEW, GYRY_DIFF_SKEW, GYRZ_DIFF_SKEW, GYRX_DIFF_KURT, GYRY_DIFF_KURT, GYRZ_DIFF_KURT,
        GYRX_MAX, GYRY_MAX, GYRZ_MAX, GYRX_MIN, GYRY_MIN, GYRZ_MIN, GYRX_MAX_ABS, GYRY_MAX_ABS, GYRZ_MAX_ABS, GYRX_MIN_ABS, GYRY_MIN_ABS, GYRZ_MIN_ABS, GYRX_RMS, GYRY_RMS, GYRZ_RMS,
        GYR_CROSS_XY, GYR_CROSS_YZ, GYR_CROSS_ZX, GYR_CROSS_XY_ABS, GYR_CROSS_YZ_ABS, GYR_CROSS_ZX_ABS, GYR_CROSS_XY_NORM, GYR_CROSS_YZ_NORM, GYR_CROSS_ZX_NORM, GYR_CROSS_XY_NORM_ABS, GYR_CROSS_YZ_NORM_ABS, GYR_CROSS_ZX_NORM_ABS,
        GYRX_FFT1, GYRX_FFT2, GYRX_FFT3, GYRX_FFT4, GYRX_FFT5, GYRX_FFT6, GYRX_FFT7, GYRX_FFT8, GYRX_FFT9, GYRX_FFT10, GYRX_FFT11, GYRX_FFT12, GYRX_FFT13, GYRX_FFT14, GYRX_FFT15, GYRX_FFT16, GYRX_FFT17, GYRX_FFT18, GYRX_FFT19,
        GYRY_FFT1, GYRY_FFT2, GYRY_FFT3, GYRY_FFT4, GYRY_FFT5, GYRY_FFT6, GYRY_FFT7, GYRY_FFT8, GYRY_FFT9, GYRY_FFT10, GYRY_FFT11, GYRY_FFT12, GYRY_FFT13, GYRY_FFT14, GYRY_FFT15, GYRY_FFT16, GYRY_FFT17, GYRY_FFT18, GYRY_FFT19,
        GYRZ_FFT1, GYRZ_FFT2, GYRZ_FFT3, GYRZ_FFT4, GYRZ_FFT5, GYRZ_FFT6, GYRZ_FFT7, GYRZ_FFT8, GYRZ_FFT9, GYRZ_FFT10, GYRZ_FFT11, GYRZ_FFT12, GYRZ_FFT13, GYRZ_FFT14, GYRZ_FFT15, GYRZ_FFT16, GYRZ_FFT17, GYRZ_FFT18, GYRZ_FFT19,
        GYRX_HIST1, GYRX_HIST2, GYRX_HIST3, GYRX_HIST4, GYRX_HIST5, GYRX_HIST6, GYRY_HIST1, GYRY_HIST2, GYRY_HIST3, GYRY_HIST4, GYRY_HIST5, GYRY_HIST6, GYRZ_HIST1, GYRZ_HIST2, GYRZ_HIST3, GYRZ_HIST4, GYRZ_HIST5, GYRZ_HIST6, PROCESSING_TIME
    }

    public FeatureExtractor(long windowSize, List<String> features, int dimensions) {

        this._windowSize = windowSize;
        this._dimensions = dimensions;

        for (String featureName : features) {

            if (featureName.contains("FFT")) {
                this._hasFFT = true;
                this._fft = new FastFourierTransformer(DftNormalization.STANDARD);
            } else if (featureName.contains("DIFF"))
                this._hasDiff = true;
            else if (featureName.contains("HIST"))
                this._hasHist = true;
            else if (featureName.contains("CROSS")) {
                this._hasCross = true;

                if (featureName.contains("NORM"))
                    this._hasNormCross = true;
            }
        }
    }


    /*public void setBinEdges(double[] edges) {
        this._binEdges = Arrays.copyOf(edges, edges.length);
    }*/

    public Map<String, Double> extractFeatures(Clip clp) {

        HashMap<String, Double> features = new HashMap<String, Double>();

        // build a copy of the clip. because it sometimes crashes suspiciously.
        Clip clip = new Clip(clp);

        // Spline Interpolation
        //List<double[]> signal = this.interpolate(clip.getValues(), clip.getTimestamps(), f_interp);
        List<double[]> signal = clip.getValues();

        // Calculating the statistical moments
        double[] mean = new double[this._dimensions];
        double[] std = new double[this._dimensions];
        double[] skewness = new double[this._dimensions];
        double[] kurtosis = new double[this._dimensions];

        for (int i = 0; i < this._dimensions; i++) {

            double[] moments = this.getMoments(signal, i);

            mean[i] = moments[0];
            std[i] = moments[1];
            skewness[i] = moments[2];
            kurtosis[i] = moments[3];
        }

        switch (clip.getType()) {

            case Clip.ACCELEROMETER:

                //features.put(Feature.ACC_NUM_SAMPLES.toString(), (double) signal.size());
                features.put(Feature.ACC_MEAN.toString(), this.getOverallMean(signal));

                features.put(Feature.ACCX_MAX.toString(), this.getMax(signal, 0));
                features.put(Feature.ACCY_MAX.toString(), this.getMax(signal, 1));
                features.put(Feature.ACCZ_MAX.toString(), this.getMax(signal, 2));

                features.put(Feature.ACCX_MIN.toString(), this.getMin(signal, 0));
                features.put(Feature.ACCY_MIN.toString(), this.getMin(signal, 1));
                features.put(Feature.ACCZ_MIN.toString(), this.getMin(signal, 2));

                features.put(Feature.ACCX_MAX_ABS.toString(), Math.abs(this.getMax(signal, 0)));
                features.put(Feature.ACCY_MAX_ABS.toString(), Math.abs(this.getMax(signal, 1)));
                features.put(Feature.ACCZ_MAX_ABS.toString(), Math.abs(this.getMax(signal, 2)));

                features.put(Feature.ACCX_MIN_ABS.toString(), Math.abs(this.getMin(signal, 0)));
                features.put(Feature.ACCY_MIN_ABS.toString(), Math.abs(this.getMin(signal, 1)));
                features.put(Feature.ACCZ_MIN_ABS.toString(), Math.abs(this.getMin(signal, 2)));

                features.put(Feature.ACCX_MEAN.toString(), mean[0]);
                features.put(Feature.ACCY_MEAN.toString(), mean[1]);
                features.put(Feature.ACCZ_MEAN.toString(), mean[2]);

                features.put(Feature.ACCX_MEAN_ABS.toString(), Math.abs(mean[0]));
                features.put(Feature.ACCY_MEAN_ABS.toString(), Math.abs(mean[1]));
                features.put(Feature.ACCZ_MEAN_ABS.toString(), Math.abs(mean[2]));

                features.put(Feature.ACCX_STD.toString(), std[0]);
                features.put(Feature.ACCY_STD.toString(), std[1]);
                features.put(Feature.ACCZ_STD.toString(), std[2]);

                features.put(Feature.ACCX_SKEW.toString(), skewness[0]);
                features.put(Feature.ACCY_SKEW.toString(), skewness[1]);
                features.put(Feature.ACCZ_SKEW.toString(), skewness[2]);

                features.put(Feature.ACCX_KURT.toString(), kurtosis[0]);
                features.put(Feature.ACCY_KURT.toString(), kurtosis[1]);
                features.put(Feature.ACCZ_KURT.toString(), kurtosis[2]);

                features.put(Feature.ACCX_RMS.toString(), this.getRMS(signal, 0));
                features.put(Feature.ACCY_RMS.toString(), this.getRMS(signal, 1));
                features.put(Feature.ACCZ_RMS.toString(), this.getRMS(signal, 2));

                break;

            case Clip.GYROSCOPE:

                //features.put(Feature.GYR_NUM_SAMPLES.toString(), (double) signal.size());
                features.put(Feature.GYR_MEAN.toString(), this.getOverallMean(signal));

                features.put(Feature.GYRX_MAX.toString(), this.getMax(signal, 0));
                features.put(Feature.GYRY_MAX.toString(), this.getMax(signal, 1));
                features.put(Feature.GYRZ_MAX.toString(), this.getMax(signal, 2));

                features.put(Feature.GYRX_MIN.toString(), this.getMin(signal, 0));
                features.put(Feature.GYRY_MIN.toString(), this.getMin(signal, 1));
                features.put(Feature.GYRZ_MIN.toString(), this.getMin(signal, 2));

                features.put(Feature.GYRX_MAX_ABS.toString(), Math.abs(this.getMax(signal, 0)));
                features.put(Feature.GYRY_MAX_ABS.toString(), Math.abs(this.getMax(signal, 1)));
                features.put(Feature.GYRZ_MAX_ABS.toString(), Math.abs(this.getMax(signal, 2)));

                features.put(Feature.GYRX_MIN_ABS.toString(), Math.abs(this.getMin(signal, 0)));
                features.put(Feature.GYRY_MIN_ABS.toString(), Math.abs(this.getMin(signal, 1)));
                features.put(Feature.GYRZ_MIN_ABS.toString(), Math.abs(this.getMin(signal, 2)));

                features.put(Feature.GYRX_MEAN.toString(), mean[0]);
                features.put(Feature.GYRY_MEAN.toString(), mean[1]);
                features.put(Feature.GYRZ_MEAN.toString(), mean[2]);

                features.put(Feature.GYRX_MEAN_ABS.toString(), Math.abs(mean[0]));
                features.put(Feature.GYRY_MEAN_ABS.toString(), Math.abs(mean[1]));
                features.put(Feature.GYRZ_MEAN_ABS.toString(), Math.abs(mean[2]));

                features.put(Feature.GYRX_STD.toString(), std[0]);
                features.put(Feature.GYRY_STD.toString(), std[1]);
                features.put(Feature.GYRZ_STD.toString(), std[2]);

                features.put(Feature.GYRX_SKEW.toString(), skewness[0]);
                features.put(Feature.GYRY_SKEW.toString(), skewness[1]);
                features.put(Feature.GYRZ_SKEW.toString(), skewness[2]);

                features.put(Feature.GYRX_KURT.toString(), kurtosis[0]);
                features.put(Feature.GYRY_KURT.toString(), kurtosis[1]);
                features.put(Feature.GYRZ_KURT.toString(), kurtosis[2]);

                features.put(Feature.GYRX_RMS.toString(), this.getRMS(signal, 0));
                features.put(Feature.GYRY_RMS.toString(), this.getRMS(signal, 1));
                features.put(Feature.GYRZ_RMS.toString(), this.getRMS(signal, 2));

                break;
        }

        if (this._hasDiff) {

            double[] diffMean = new double[this._dimensions];
            double[] diffStd = new double[this._dimensions];
            double[] diffSkewness = new double[this._dimensions];
            double[] diffKurtosis = new double[this._dimensions];

            List<double[]> signalDiff = this.getDiff(signal);

            // Calculating the statistical moments of the difference signal
            for (int i = 0; i < this._dimensions; i++) {
                double[] moments = this.getMoments(signalDiff, i);
                diffMean[i] = moments[0];
                diffStd[i] = moments[1];
                diffSkewness[i] = moments[2];
                diffKurtosis[i] = moments[3];
            }

            switch (clip.getType()) {

                case Clip.ACCELEROMETER:

                    features.put(Feature.ACCX_DIFF_MEAN.toString(), diffMean[0]);
                    features.put(Feature.ACCY_DIFF_MEAN.toString(), diffMean[1]);
                    features.put(Feature.ACCZ_DIFF_MEAN.toString(), diffMean[2]);

                    features.put(Feature.ACCX_DIFF_STD.toString(), diffStd[0]);
                    features.put(Feature.ACCY_DIFF_STD.toString(), diffStd[1]);
                    features.put(Feature.ACCZ_DIFF_STD.toString(), diffStd[2]);

                    features.put(Feature.ACCX_DIFF_SKEW.toString(), diffSkewness[0]);
                    features.put(Feature.ACCY_DIFF_SKEW.toString(), diffSkewness[1]);
                    features.put(Feature.ACCZ_DIFF_SKEW.toString(), diffSkewness[2]);

                    features.put(Feature.ACCX_DIFF_KURT.toString(), diffKurtosis[0]);
                    features.put(Feature.ACCY_DIFF_KURT.toString(), diffKurtosis[1]);
                    features.put(Feature.ACCZ_DIFF_KURT.toString(), diffKurtosis[2]);

                    break;

                case Clip.GYROSCOPE:

                    features.put(Feature.GYRX_DIFF_MEAN.toString(), diffMean[0]);
                    features.put(Feature.GYRY_DIFF_MEAN.toString(), diffMean[1]);
                    features.put(Feature.GYRZ_DIFF_MEAN.toString(), diffMean[2]);

                    features.put(Feature.GYRX_DIFF_STD.toString(), diffStd[0]);
                    features.put(Feature.GYRY_DIFF_STD.toString(), diffStd[1]);
                    features.put(Feature.GYRZ_DIFF_STD.toString(), diffStd[2]);

                    features.put(Feature.GYRX_DIFF_SKEW.toString(), diffSkewness[0]);
                    features.put(Feature.GYRY_DIFF_SKEW.toString(), diffSkewness[1]);
                    features.put(Feature.GYRZ_DIFF_SKEW.toString(), diffSkewness[2]);

                    features.put(Feature.GYRX_DIFF_KURT.toString(), diffKurtosis[0]);
                    features.put(Feature.GYRY_DIFF_KURT.toString(), diffKurtosis[1]);
                    features.put(Feature.GYRZ_DIFF_KURT.toString(), diffKurtosis[2]);

                    break;
            }
        }

        if (this._hasHist) {

            // histogram of zscore values

            int[][] hist = new int[this._dimensions][this._binEdges.length - 1];

            List<double[]> signalZScore = this.getZScore(signal, mean, std);

            for (int i = 0; i < this._dimensions; i++) {
                for (int j = 0; j < this._binEdges.length - 1; j++)
                    hist[i][j] = 0;

                for (double[] aSignalZScore : signalZScore) {

                    int bin = (int) ((aSignalZScore[i] - this._binEdges[0]) / (this._binEdges[1] - this._binEdges[0]));

                    if ((bin < this._binEdges.length - 1) && (bin >= 0))
                        hist[i][bin]++; // values outside the range are neglected
                }
            }

            // TODO
            // Add another set of histograms on raw signals (not zscore)
            // TBD also on MATLAB side

            switch (clip.getType()) {

                case Clip.ACCELEROMETER:

                    features.put(Feature.ACCX_HIST1.toString(), (double) hist[0][0]);
                    features.put(Feature.ACCX_HIST2.toString(), (double) hist[0][1]);
                    features.put(Feature.ACCX_HIST3.toString(), (double) hist[0][2]);
                    features.put(Feature.ACCX_HIST4.toString(), (double) hist[0][3]);
                    features.put(Feature.ACCX_HIST5.toString(), (double) hist[0][4]);
                    features.put(Feature.ACCX_HIST6.toString(), (double) hist[0][5]);

                    features.put(Feature.ACCY_HIST1.toString(), (double) hist[1][0]);
                    features.put(Feature.ACCY_HIST2.toString(), (double) hist[1][1]);
                    features.put(Feature.ACCY_HIST3.toString(), (double) hist[1][2]);
                    features.put(Feature.ACCY_HIST4.toString(), (double) hist[1][3]);
                    features.put(Feature.ACCY_HIST5.toString(), (double) hist[1][4]);
                    features.put(Feature.ACCY_HIST6.toString(), (double) hist[1][5]);

                    features.put(Feature.ACCZ_HIST1.toString(), (double) hist[2][0]);
                    features.put(Feature.ACCZ_HIST2.toString(), (double) hist[2][1]);
                    features.put(Feature.ACCZ_HIST3.toString(), (double) hist[2][2]);
                    features.put(Feature.ACCZ_HIST4.toString(), (double) hist[2][3]);
                    features.put(Feature.ACCZ_HIST5.toString(), (double) hist[2][4]);
                    features.put(Feature.ACCZ_HIST6.toString(), (double) hist[2][5]);

                    break;

                case Clip.GYROSCOPE:

                    features.put(Feature.GYRX_HIST1.toString(), (double) hist[0][0]);
                    features.put(Feature.GYRX_HIST2.toString(), (double) hist[0][1]);
                    features.put(Feature.GYRX_HIST3.toString(), (double) hist[0][2]);
                    features.put(Feature.GYRX_HIST4.toString(), (double) hist[0][3]);
                    features.put(Feature.GYRX_HIST5.toString(), (double) hist[0][4]);
                    features.put(Feature.GYRX_HIST6.toString(), (double) hist[0][5]);

                    features.put(Feature.GYRY_HIST1.toString(), (double) hist[1][0]);
                    features.put(Feature.GYRY_HIST2.toString(), (double) hist[1][1]);
                    features.put(Feature.GYRY_HIST3.toString(), (double) hist[1][2]);
                    features.put(Feature.GYRY_HIST4.toString(), (double) hist[1][3]);
                    features.put(Feature.GYRY_HIST5.toString(), (double) hist[1][4]);
                    features.put(Feature.GYRY_HIST6.toString(), (double) hist[1][5]);

                    features.put(Feature.GYRZ_HIST1.toString(), (double) hist[2][0]);
                    features.put(Feature.GYRZ_HIST2.toString(), (double) hist[2][1]);
                    features.put(Feature.GYRZ_HIST3.toString(), (double) hist[2][2]);
                    features.put(Feature.GYRZ_HIST4.toString(), (double) hist[2][3]);
                    features.put(Feature.GYRZ_HIST5.toString(), (double) hist[2][4]);
                    features.put(Feature.GYRZ_HIST6.toString(), (double) hist[2][5]);

                    break;
            }
        }

        if (this._hasCross) {
            double[] cross = new double[this._dimensions];

            for (int i = 0; i < this._dimensions; i++)
                cross[i] = 0;

            if (this._dimensions == 3) {
                cross = this.get3DInnerProds(signal);

                switch (clip.getType()) {

                    case Clip.ACCELEROMETER:

                        features.put(Feature.ACC_CROSS_XY.toString(), cross[0]);
                        features.put(Feature.ACC_CROSS_YZ.toString(), cross[1]);
                        features.put(Feature.ACC_CROSS_ZX.toString(), cross[2]);

                        features.put(Feature.ACC_CROSS_XY_ABS.toString(), Math.abs(cross[0]));
                        features.put(Feature.ACC_CROSS_YZ_ABS.toString(), Math.abs(cross[1]));
                        features.put(Feature.ACC_CROSS_ZX_ABS.toString(), Math.abs(cross[2]));

                        break;

                    case Clip.GYROSCOPE:

                        features.put(Feature.GYR_CROSS_XY.toString(), cross[0]);
                        features.put(Feature.GYR_CROSS_YZ.toString(), cross[1]);
                        features.put(Feature.GYR_CROSS_ZX.toString(), cross[2]);

                        features.put(Feature.GYR_CROSS_XY_ABS.toString(), Math.abs(cross[0]));
                        features.put(Feature.GYR_CROSS_YZ_ABS.toString(), Math.abs(cross[1]));
                        features.put(Feature.GYR_CROSS_ZX_ABS.toString(), Math.abs(cross[2]));

                        break;
                }
            } else
                Log.e("PR",
                        "FeatureExtractor: Calculating cross-dimensional inner-products for a non-3D signal - values set to zero!");
        }

        if (this._hasNormCross) {
            double[] crossNorm = new double[this._dimensions];

            for (int i = 0; i < this._dimensions; i++)
                crossNorm[i] = 0;

            if (this._dimensions == 3) {
                crossNorm = this.get3DNormInnerProds(signal);

                switch (clip.getType()) {
                    case Clip.ACCELEROMETER:
                        features.put(Feature.ACC_CROSS_XY_NORM.toString(), crossNorm[0]);
                        features.put(Feature.ACC_CROSS_YZ_NORM.toString(), crossNorm[1]);
                        features.put(Feature.ACC_CROSS_ZX_NORM.toString(), crossNorm[2]);

                        features.put(Feature.ACC_CROSS_XY_NORM_ABS.toString(), Math.abs(crossNorm[0]));
                        features.put(Feature.ACC_CROSS_YZ_NORM_ABS.toString(), Math.abs(crossNorm[1]));
                        features.put(Feature.ACC_CROSS_ZX_NORM_ABS.toString(), Math.abs(crossNorm[2]));

                        break;
                    case Clip.GYROSCOPE:
                        features.put(Feature.GYR_CROSS_XY_NORM.toString(), crossNorm[0]);
                        features.put(Feature.GYR_CROSS_YZ_NORM.toString(), crossNorm[1]);
                        features.put(Feature.GYR_CROSS_ZX_NORM.toString(), crossNorm[2]);

                        features.put(Feature.GYR_CROSS_XY_NORM_ABS.toString(), Math.abs(crossNorm[0]));
                        features.put(Feature.GYR_CROSS_YZ_NORM_ABS.toString(), Math.abs(crossNorm[1]));
                        features.put(Feature.GYR_CROSS_ZX_NORM_ABS.toString(), Math.abs(crossNorm[2]));

                        break;
                }

            } else
                Log.e("Warning",
                        "Calculating cross-dimensional inner-products for a non-3D signal - values set to zero!");
        }


        if (this._hasFFT) {

            double[] fftvalues_x = getFFT(signal, 0);
            double[] fftvalues_y = getFFT(signal, 1);
            double[] fftvalues_z = getFFT(signal, 2);

            switch (clip.getType()) {
                case Clip.ACCELEROMETER:
                    features.put(Feature.ACCX_FFT1.toString(), fftvalues_x[0]);
                    features.put(Feature.ACCX_FFT2.toString(), fftvalues_x[1]);
                    features.put(Feature.ACCX_FFT3.toString(), fftvalues_x[2]);
                    features.put(Feature.ACCX_FFT4.toString(), fftvalues_x[3]);
                    features.put(Feature.ACCX_FFT5.toString(), fftvalues_x[4]);
                    features.put(Feature.ACCX_FFT6.toString(), fftvalues_x[5]);
                    features.put(Feature.ACCX_FFT7.toString(), fftvalues_x[6]);
                    features.put(Feature.ACCX_FFT8.toString(), fftvalues_x[7]);
                    features.put(Feature.ACCX_FFT9.toString(), fftvalues_x[8]);
                    features.put(Feature.ACCX_FFT10.toString(), fftvalues_x[9]);
                    features.put(Feature.ACCX_FFT11.toString(), fftvalues_x[10]);
                    features.put(Feature.ACCX_FFT12.toString(), fftvalues_x[11]);
                    features.put(Feature.ACCX_FFT13.toString(), fftvalues_x[12]);
                    features.put(Feature.ACCX_FFT14.toString(), fftvalues_x[13]);
                    features.put(Feature.ACCX_FFT15.toString(), fftvalues_x[14]);
                    features.put(Feature.ACCX_FFT16.toString(), fftvalues_x[15]);
                    features.put(Feature.ACCX_FFT17.toString(), fftvalues_x[16]);
                    features.put(Feature.ACCX_FFT18.toString(), fftvalues_x[17]);
                    features.put(Feature.ACCX_FFT19.toString(), fftvalues_x[18]);

                    features.put(Feature.ACCY_FFT1.toString(), fftvalues_y[0]);
                    features.put(Feature.ACCY_FFT2.toString(), fftvalues_y[1]);
                    features.put(Feature.ACCY_FFT3.toString(), fftvalues_y[2]);
                    features.put(Feature.ACCY_FFT4.toString(), fftvalues_y[3]);
                    features.put(Feature.ACCY_FFT5.toString(), fftvalues_y[4]);
                    features.put(Feature.ACCY_FFT6.toString(), fftvalues_y[5]);
                    features.put(Feature.ACCY_FFT7.toString(), fftvalues_y[6]);
                    features.put(Feature.ACCY_FFT8.toString(), fftvalues_y[7]);
                    features.put(Feature.ACCY_FFT9.toString(), fftvalues_y[8]);
                    features.put(Feature.ACCY_FFT10.toString(), fftvalues_y[9]);
                    features.put(Feature.ACCY_FFT11.toString(), fftvalues_y[10]);
                    features.put(Feature.ACCY_FFT12.toString(), fftvalues_y[11]);
                    features.put(Feature.ACCY_FFT13.toString(), fftvalues_y[12]);
                    features.put(Feature.ACCY_FFT14.toString(), fftvalues_y[13]);
                    features.put(Feature.ACCY_FFT15.toString(), fftvalues_y[14]);
                    features.put(Feature.ACCY_FFT16.toString(), fftvalues_y[15]);
                    features.put(Feature.ACCY_FFT17.toString(), fftvalues_y[16]);
                    features.put(Feature.ACCY_FFT18.toString(), fftvalues_y[17]);
                    features.put(Feature.ACCY_FFT19.toString(), fftvalues_y[18]);

                    features.put(Feature.ACCZ_FFT1.toString(), fftvalues_z[0]);
                    features.put(Feature.ACCZ_FFT2.toString(), fftvalues_z[1]);
                    features.put(Feature.ACCZ_FFT3.toString(), fftvalues_z[2]);
                    features.put(Feature.ACCZ_FFT4.toString(), fftvalues_z[3]);
                    features.put(Feature.ACCZ_FFT5.toString(), fftvalues_z[4]);
                    features.put(Feature.ACCZ_FFT6.toString(), fftvalues_z[5]);
                    features.put(Feature.ACCZ_FFT7.toString(), fftvalues_z[6]);
                    features.put(Feature.ACCZ_FFT8.toString(), fftvalues_z[7]);
                    features.put(Feature.ACCZ_FFT9.toString(), fftvalues_z[8]);
                    features.put(Feature.ACCZ_FFT10.toString(), fftvalues_z[9]);
                    features.put(Feature.ACCZ_FFT11.toString(), fftvalues_z[10]);
                    features.put(Feature.ACCZ_FFT12.toString(), fftvalues_z[11]);
                    features.put(Feature.ACCZ_FFT13.toString(), fftvalues_z[12]);
                    features.put(Feature.ACCZ_FFT14.toString(), fftvalues_z[13]);
                    features.put(Feature.ACCZ_FFT15.toString(), fftvalues_z[14]);
                    features.put(Feature.ACCZ_FFT16.toString(), fftvalues_z[15]);
                    features.put(Feature.ACCZ_FFT17.toString(), fftvalues_z[16]);
                    features.put(Feature.ACCZ_FFT18.toString(), fftvalues_z[17]);
                    features.put(Feature.ACCZ_FFT19.toString(), fftvalues_z[18]);

                    break;
                case Clip.GYROSCOPE:
                    features.put(Feature.GYRX_FFT1.toString(), fftvalues_x[0]);
                    features.put(Feature.GYRX_FFT2.toString(), fftvalues_x[1]);
                    features.put(Feature.GYRX_FFT3.toString(), fftvalues_x[2]);
                    features.put(Feature.GYRX_FFT4.toString(), fftvalues_x[3]);
                    features.put(Feature.GYRX_FFT5.toString(), fftvalues_x[4]);
                    features.put(Feature.GYRX_FFT6.toString(), fftvalues_x[5]);
                    features.put(Feature.GYRX_FFT7.toString(), fftvalues_x[6]);
                    features.put(Feature.GYRX_FFT8.toString(), fftvalues_x[7]);
                    features.put(Feature.GYRX_FFT9.toString(), fftvalues_x[8]);
                    features.put(Feature.GYRX_FFT10.toString(), fftvalues_x[9]);
                    features.put(Feature.GYRX_FFT11.toString(), fftvalues_x[10]);
                    features.put(Feature.GYRX_FFT12.toString(), fftvalues_x[11]);
                    features.put(Feature.GYRX_FFT13.toString(), fftvalues_x[12]);
                    features.put(Feature.GYRX_FFT14.toString(), fftvalues_x[13]);
                    features.put(Feature.GYRX_FFT15.toString(), fftvalues_x[14]);
                    features.put(Feature.GYRX_FFT16.toString(), fftvalues_x[15]);
                    features.put(Feature.GYRX_FFT17.toString(), fftvalues_x[16]);
                    features.put(Feature.GYRX_FFT18.toString(), fftvalues_x[17]);
                    features.put(Feature.GYRX_FFT19.toString(), fftvalues_x[18]);

                    features.put(Feature.GYRY_FFT1.toString(), fftvalues_y[0]);
                    features.put(Feature.GYRY_FFT2.toString(), fftvalues_y[1]);
                    features.put(Feature.GYRY_FFT3.toString(), fftvalues_y[2]);
                    features.put(Feature.GYRY_FFT4.toString(), fftvalues_y[3]);
                    features.put(Feature.GYRY_FFT5.toString(), fftvalues_y[4]);
                    features.put(Feature.GYRY_FFT6.toString(), fftvalues_y[5]);
                    features.put(Feature.GYRY_FFT7.toString(), fftvalues_y[6]);
                    features.put(Feature.GYRY_FFT8.toString(), fftvalues_y[7]);
                    features.put(Feature.GYRY_FFT9.toString(), fftvalues_y[8]);
                    features.put(Feature.GYRY_FFT10.toString(), fftvalues_y[9]);
                    features.put(Feature.GYRY_FFT11.toString(), fftvalues_y[10]);
                    features.put(Feature.GYRY_FFT12.toString(), fftvalues_y[11]);
                    features.put(Feature.GYRY_FFT13.toString(), fftvalues_y[12]);
                    features.put(Feature.GYRY_FFT14.toString(), fftvalues_y[13]);
                    features.put(Feature.GYRY_FFT15.toString(), fftvalues_y[14]);
                    features.put(Feature.GYRY_FFT16.toString(), fftvalues_y[15]);
                    features.put(Feature.GYRY_FFT17.toString(), fftvalues_y[16]);
                    features.put(Feature.GYRY_FFT18.toString(), fftvalues_y[17]);
                    features.put(Feature.GYRY_FFT19.toString(), fftvalues_y[18]);

                    features.put(Feature.GYRZ_FFT1.toString(), fftvalues_z[0]);
                    features.put(Feature.GYRZ_FFT2.toString(), fftvalues_z[1]);
                    features.put(Feature.GYRZ_FFT3.toString(), fftvalues_z[2]);
                    features.put(Feature.GYRZ_FFT4.toString(), fftvalues_z[3]);
                    features.put(Feature.GYRZ_FFT5.toString(), fftvalues_z[4]);
                    features.put(Feature.GYRZ_FFT6.toString(), fftvalues_z[5]);
                    features.put(Feature.GYRZ_FFT7.toString(), fftvalues_z[6]);
                    features.put(Feature.GYRZ_FFT8.toString(), fftvalues_z[7]);
                    features.put(Feature.GYRZ_FFT9.toString(), fftvalues_z[8]);
                    features.put(Feature.GYRZ_FFT10.toString(), fftvalues_z[9]);
                    features.put(Feature.GYRZ_FFT11.toString(), fftvalues_z[10]);
                    features.put(Feature.GYRZ_FFT12.toString(), fftvalues_z[11]);
                    features.put(Feature.GYRZ_FFT13.toString(), fftvalues_z[12]);
                    features.put(Feature.GYRZ_FFT14.toString(), fftvalues_z[13]);
                    features.put(Feature.GYRZ_FFT15.toString(), fftvalues_z[14]);
                    features.put(Feature.GYRZ_FFT16.toString(), fftvalues_z[15]);
                    features.put(Feature.GYRZ_FFT17.toString(), fftvalues_z[16]);
                    features.put(Feature.GYRZ_FFT18.toString(), fftvalues_z[17]);
                    features.put(Feature.GYRZ_FFT19.toString(), fftvalues_z[18]);

                    break;
            }
        }

        return features;
    }

    // TODO: This function probably has a bug - the window size is neglected
    // It does actually, but the same thing also exists on the MATLAB side.
    // I might get rid of interpolation all together.

/*
    private List<double[]> interpolate(List<double[]> signal, List<Long> ts, int freq) {

        List<double[]> signalOut = new ArrayList<double[]>();

        int N = ts.size();

        if (N < 2) {
            Log.e("INTERP", "Warning: Interpolation was not done due to small number of samples.");
            return signal;
        }

        double stepSize = 1e9 / (double) freq; // step size in nanoseconds

        // checking if the timestamps are incremental
        // if not, the data point is removed

        List<Long> t2 = new ArrayList<Long>();

        List<double[]> signal2 = new ArrayList<double[]>();

        t2.add(ts.get(0));

        signal2.add(Arrays.copyOf(signal.get(0), signal.get(0).length));

        for (int j = 1; j < N; j++) {
            if (ts.get(j) > t2.get(t2.size() - 1)) {
                t2.add(ts.get(j));
                signal2.add(Arrays.copyOf(signal.get(j), signal.get(j).length));
            } else {
                Log.e("PR", "FeatureExtractor: Non-incremental timestamp found and removed!");
            }
        }

        int N2 = signal2.size();

        // converting time instances to double
        long tStart = t2.get(0);
        double[] tDouble = new double[N2];

        // getting rid of big numbers
        for (int j = 0; j < N2; j++)
            tDouble[j] = t2.get(j) - tStart;

        // calculating the number of samples to be interpolated
        int nSamp = (int) Math.floor(tDouble[N2 - 1] / stepSize);

        // creating new regular time instances for interpolation
        double[] tNew = new double[nSamp];
        for (int j = 0; j < nSamp; j++)
            tNew[j] = tDouble[N2 - 1] - (double) j * stepSize;

        double[][] signalOutTemp = new double[nSamp][this._dimensions];

        for (int i = 0; i < this._dimensions; i++) {

            // building a 1D array for the current axis
            double[] signal1D = new double[N2];
            for (int j = 0; j < N2; j++)
                signal1D[j] = signal2.get(j)[i];

            // spline interpolation
            SplineInterpolator interp = new SplineInterpolator();
            PolynomialSplineFunction func = interp.interpolate(tDouble, signal1D);

            // interpolating onto new instances
            for (int j = 0; j < nSamp; j++)
                signalOutTemp[j][i] = func.value(tNew[j]);
        }

        for (int i = 0; i < nSamp; i++) {
            signalOut.add(new double[this._dimensions]);
            signalOut.set(signalOut.size() - 1, signalOutTemp[i]);
        }

        return signalOut;
    }

    */

    private List<double[]> getDiff(List<double[]> signal) {

        List<double[]> signalDiff = new ArrayList<double[]>();

        int N = signal.size();

        for (int i = 0; i < N - 1; i++) {
            double[] sig = signal.get(i);
            double[] sigNext = signal.get(i + 1);

            double[] sigDiff = new double[sig.length];

            for (int j = 0; j < sig.length; j++)
                sigDiff[j] = sigNext[j] - sig[j];

            signalDiff.add(sigDiff);
        }

        return signalDiff;
    }

    private List<double[]> getZScore(List<double[]> signal, double[] mean, double[] std) {

        List<double[]> signalZScore = new ArrayList<double[]>();

        for (double[] sig : signal) {

            double[] sigZScore = new double[this._dimensions];

            for (int j = 0; j < this._dimensions; j++)
                sigZScore[j] = (sig[j] - mean[j]) / std[j];

            signalZScore.add(sigZScore);
        }

        return signalZScore;
    }

    // This method will calculate mean, standard deviation, skewness, and kurtosis.
    // each member of the list is one statistical moment, which consists of an
    // array, with each element accounting for one dimension

    private double[] getMoments(List<double[]> signal, int axis) {

        int N = signal.size();

        double[] signalArray = new double[N];

        for (int i = 0; i < N; i++) {
            signalArray[i] = signal.get(i)[axis];
        }

        // Calculation of moments is not possible with less than 2 samples. Returning zeros in that case.
        if (N < 2)
            return new double[]{0.0, 0.0, 0.0, 0.0};

        double sum = 0.0;

        for (int i = 0; i < N; i++)
            sum += signalArray[i];

        double mean = sum / N;

        double m2 = 0.0;
        double m3 = 0.0;
        double m4 = 0.0;

        for (int i = 0; i < N; i++) {
            double t2 = (signalArray[i] - mean) * (signalArray[i] - mean);
            m2 += t2;

            double t3 = t2 * (signalArray[i] - mean);
            m3 += t3;

            double t4 = t3 * (signalArray[i] - mean);
            m4 += t4;
        }

        double std = Math.sqrt(m2 / (N - 1)); // unbiased estimator

        m2 /= N;
        m3 /= N;
        m4 /= N;

        double skewness = m3 / (std * std * std); // unbiased estimator

        double kurtosis = m4 / (m2 * m2) - 3; // unbiased estimator

        return new double[]{mean, std, skewness, kurtosis};

    }

    // overall mean of squares
    private double getOverallMean(List<double[]> signal) {

        int N = signal.size();

        double[][] signalArray = new double[N][this._dimensions];

        for (int i = 0; i < N; i++) {
            signalArray[i] = signal.get(i);
        }

        double ms = 0;

        for (int i = 0; i < N; i++)
            for (int j = 0; j < this._dimensions; j++)
                ms += signalArray[i][j] * signalArray[i][j] / this._dimensions;

        ms /= N;

        return ms;
    }

    private double getRMS(List<double[]> signal, int axis) {

        int N = signal.size();

        double rms = 0;

        for (double[] aSignal : signal) rms += aSignal[axis] * aSignal[axis];

        rms /= (double) N;

        return rms;
    }

    private double getMax(List<double[]> signal, int axis) {

        int N = signal.size();

        if (N == 0)
            return 0;

        double max = signal.get(0)[axis];

        for (int i = 1; i < N; i++) {
            if (max < signal.get(i)[axis])
                max = signal.get(i)[axis];
        }

        return max;
    }

    private double getMin(List<double[]> signal, int axis) {

        int N = signal.size();

        if (N == 0)
            return 0;

        double min = signal.get(0)[axis];

        for (int i = 1; i < N; i++) {
            if (min > signal.get(i)[axis])
                min = signal.get(i)[axis];
        }

        return min;
    }

    private double[] get3DInnerProds(List<double[]> signal) {

        // This feature only works for 3D signals (acceleration, magnetic field,
        // etc).

        double[] innerProds = {0, 0, 0};

        // double-check for the dimension - returning zeros if it is different from three
        if (this._dimensions != 3)
            return innerProds;

        int N = signal.size();

        for (double[] aSignal : signal) innerProds[0] += aSignal[0] * aSignal[1];
        innerProds[0] /= (double) N; // mean

        for (double[] aSignal : signal) innerProds[1] += aSignal[1] * aSignal[2];
        innerProds[1] /= (double) N; // mean

        for (double[] aSignal : signal) innerProds[2] += aSignal[2] * aSignal[0];
        innerProds[2] /= (double) N; // mean

        return innerProds;
    }

    private double[] get3DNormInnerProds(List<double[]> signal) {

        double[] innerProds = {0, 0, 0};

        // double-check for the dimension - returning zeros if it is different from three
        if (this._dimensions != 3)
            return innerProds;

        int N = signal.size();

        double[] magnitude = new double[N];

        for (int j = 0; j < N; j++) {
            magnitude[j] = (signal.get(j)[0] * signal.get(j)[0])
                    + (signal.get(j)[1] * signal.get(j)[1])
                    + (signal.get(j)[2] * signal.get(j)[2]);
        }

        for (int j = 0; j < N; j++)
            innerProds[0] += signal.get(j)[0] * signal.get(j)[1] / magnitude[j];
        innerProds[0] /= (double) N; // mean

        for (int j = 0; j < N; j++)
            innerProds[1] += signal.get(j)[1] * signal.get(j)[2] / magnitude[j];
        innerProds[1] /= (double) N; // mean

        for (int j = 0; j < N; j++)
            innerProds[2] += signal.get(j)[2] * signal.get(j)[0] / magnitude[j];
        innerProds[2] /= (double) N; // mean

        return innerProds;
    }

    private double[] getFFT(List<double[]> signal, int axis) {

        double[] fft_values = new double[this._NFFT];
        for (int i=1; i<fft_values.length; i++)
            fft_values[i] = 0.0;

        int N = signal.size();
        if (N<2*this._NFFT+1) {
            Log.e("INFO", "Cannot calculate FFT - the number of samples is not enough!");
            return fft_values;
        }

        int n_zeropad = 256;
        if (N>n_zeropad) {
            Log.e("INFO", "Too many samples!");
            return fft_values;
        }

        double[] signalArray = new double[n_zeropad];
        for (int i = 0; i<n_zeropad; i++)
            signalArray[i] = 0;

        for (int i = 0; i < N; i++) {
            signalArray[i] = signal.get(i)[axis];
        }

        Complex[] fft_complex = _fft.transform(signalArray, TransformType.FORWARD);

        //double sum = 0.0;
        for (int i=0; i<this._NFFT; i++) {
            fft_values[i] = fft_complex[i+1].abs(); //neglecting the first coefficient as it represents the energy
            //sum += fft_values[i];
        }

        //for (int i=0; i<this._NFFT; i++)
            //fft_values[i] /= sum;


        return fft_values;

    }

}