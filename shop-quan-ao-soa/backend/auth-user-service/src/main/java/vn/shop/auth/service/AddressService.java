package vn.shop.auth.service;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.shop.auth.dto.AuthDtos.*;
import vn.shop.auth.entity.Address;
import vn.shop.auth.repository.*;
import vn.shop.common.*;

@Service
public class AddressService {
    private final AddressRepository addresses;
    private final UserRepository users;
    public AddressService(AddressRepository addresses,UserRepository users) { this.addresses=addresses; this.users=users; }
    @Transactional(readOnly=true) public List<AddressView> list() { return addresses.findByUserIdOrderByIdDesc(Caller.id()).stream().map(AddressView::of).toList(); }
    @Transactional public AddressView save(Long id,AddressInput input) {
        long userId=Caller.id(); users.lock(userId).orElseThrow(ApiException::missing);
        Address a=id==null ? new Address() : addresses.findByIdAndUserId(id,userId).orElseThrow(ApiException::missing);
        var existing=addresses.findByUserIdOrderByIdDesc(userId);
        if(id==null && existing.size()>=20) throw new ApiException(400,"Tối đa 20 địa chỉ");
        boolean isDefault=input.defaultAddress() || existing.isEmpty();
        if(isDefault) existing.forEach(x -> { x.defaultAddress=false; addresses.save(x); });
        a.userId=userId; a.recipient=input.recipient().trim(); a.phone=input.phone().trim(); a.detail=input.detail().trim(); a.defaultAddress=isDefault; a.updatedAt=Instant.now();
        return AddressView.of(addresses.save(a));
    }
    @Transactional public void delete(long id) {
        long userId=Caller.id(); users.lock(userId).orElseThrow(ApiException::missing);
        var a=addresses.findByIdAndUserId(id,userId).orElseThrow(ApiException::missing); addresses.delete(a);
    }
}
