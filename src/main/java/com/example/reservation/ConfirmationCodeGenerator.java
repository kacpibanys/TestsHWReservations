package com.example.reservation;

public interface ConfirmationCodeGenerator {

    /**

     Generuje unikalny kod potwierdzenia.
     Format: "RES-XXXXXXXX" (np. "RES-A1B2C3D4")*/
    String generate();
}