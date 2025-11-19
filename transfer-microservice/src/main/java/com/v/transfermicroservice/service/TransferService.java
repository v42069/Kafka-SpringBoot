package com.v.transfermicroservice.service;

import com.appsdeveloperblog.estore.transfers.model.TransferRestModel;

public interface TransferService {
    public boolean transfer(TransferRestModel productPaymentRestModel);
}
