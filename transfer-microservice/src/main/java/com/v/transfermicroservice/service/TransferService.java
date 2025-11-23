package com.v.transfermicroservice.service;

import com.v.transfermicroservice.model.TransferRestModel;

public interface TransferService {
    public boolean transfer(TransferRestModel productPaymentRestModel);
}
