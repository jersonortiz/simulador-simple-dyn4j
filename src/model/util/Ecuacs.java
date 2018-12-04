/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.util;

/**
 *
 * @author jerson
 */
public final class Ecuacs {

    public static double calcW(double p) {
        return (2 * Math.PI) / p;
    }

    public static double calcW(double k, double m) {
        return Math.sqrt(k / m);
    }

    public static double calcP(double k, double m) {
        return Ecuacs.calcP(Ecuacs.calcW(k, m));
    }

    public static double calcP(double w) {
        return (2 * Math.PI) / w;

    }

    public static double calcK(double w, double m) {
        return m * Math.pow(w, 2);
    }

    public static double calcEP(double k, double x) {
        return (1 / 2) * k * Math.pow(x, 2);
    }

    public static double calcEP(double m, double w, double x) {
        return Ecuacs.calcEP(Ecuacs.calcK(w, m), x);
    }

    public static double calcEC(double m, double w, double A, double x) {
        return Ecuacs.calcEC(Ecuacs.calcK(w, m), A, x);
    }

    public static double calcEC(double k, double A, double x) {
        return (1 / 2) * k * (Math.pow(A, 2) - Math.pow(x, 2));
    }

    public static double calcE(double k, double A) {
        return (1 / 2) * k * Math.pow(A, 2);
    }

    public static double calcE(double m, double w, double A) {
        return Ecuacs.calcE(Ecuacs.calcK(w, m), A);
    }

    public static double calcX(double A, double w, double t, double fi) {
        return A * Math.cos(w * t + fi);
    }
    
    public static double calcV(double A, double w, double t, double fi){
        return (-1)*w*Ecuacs.calcX(A, w, t, fi);
    }
    
    public static double calcAcel(double A, double w, double t, double fi){
        return w*Ecuacs.calcV(A, w, t, fi);
    }
    
    public static double calcAcel(double w , double x){
        return -Math.pow(w, 2)*x;
    }
}
