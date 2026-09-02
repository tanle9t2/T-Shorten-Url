package com.tanle.t_shorten_url.service;

import com.google.zxing.WriterException;

import java.io.IOException;

public interface QRCodeService {
    String generateQrCode(String url);
}
