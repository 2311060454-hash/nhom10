package vn.shop.reporting;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.shop.common.*;
import vn.shop.reporting.GuestContactDtos.*;

@Service
public class GuestContactService {
 private final GuestContactRepository contacts;private final SupportGuardRepository guard;
 public GuestContactService(GuestContactRepository contacts,SupportGuardRepository guard){this.contacts=contacts;this.guard=guard;}
 private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private static View view(GuestContact c){return new View(c.id,c.fullName,c.email,c.phone,c.subject,c.message,c.state,c.staffNote,c.actorId,c.createdAt,c.updatedAt);}
 @Transactional public Receipt create(String key,Create input){
  if(key==null||!key.matches("[A-Za-z0-9_-]{8,100}"))throw new ApiException(400,"Idempotency-Key không hợp lệ");
  String name=input.fullName().trim(),email=input.email().trim().toLowerCase(Locale.ROOT),phone=input.phone()==null||input.phone().isBlank()?null:input.phone().trim(),subject=input.subject().trim(),message=input.message().trim();
  if(name.length()<2||subject.length()<5||message.length()<10)throw new ApiException(400,"Nội dung liên hệ quá ngắn");
  String fingerprint=hash(name+"\u0000"+email+"\u0000"+phone+"\u0000"+subject+"\u0000"+message);
  guard.lock();
  var old=contacts.findByEmailAndRequestKey(email,key);
  if(old.isPresent()){if(!old.get().requestHash.equals(fingerprint))throw new ApiException(409,"Mã gửi đã dùng với nội dung khác");return new Receipt(old.get().id,old.get().createdAt);}
  if(contacts.countByEmailAndCreatedAtAfter(email,Instant.now().minusSeconds(3600))>=3)throw new ApiException(429,"Mỗi email chỉ gửi tối đa 3 yêu cầu trong một giờ");
  GuestContact c=new GuestContact();c.requestKey=key;c.requestHash=fingerprint;c.fullName=name;c.email=email;c.phone=phone==null||phone.isBlank()?null:phone;c.subject=subject;c.message=message;
  c=contacts.saveAndFlush(c);return new Receipt(c.id,c.createdAt);
 }
 @Transactional(readOnly=true) public PageResult<View> list(String state,String q,int page){
  if(state!=null&&!Set.of("OPEN","IN_PROGRESS","RESOLVED").contains(state))throw new ApiException(400,"Trạng thái liên hệ không hợp lệ");
  var data=contacts.search(state,q==null||q.isBlank()?null:q.trim(),PageRequest.of(page,20,Sort.by("updatedAt").descending().and(Sort.by("id").descending())));
  return new PageResult<>(data.map(GuestContactService::view).getContent(),data.getTotalElements(),data.getTotalPages(),page,20,data.isLast());
 }
 @Transactional(readOnly=true) public View get(long id){return view(contacts.findById(id).orElseThrow(ApiException::missing));}
 @Transactional public View update(long id,long actor,Update input){
  guard.lock();GuestContact c=contacts.findById(id).orElseThrow(ApiException::missing);
  String next=input.state(),note=input.note()==null?"":input.note().trim();
  if(next.equals("RESOLVED")&&note.isBlank())throw new ApiException(400,"Cần ghi chú cách đã xử lý liên hệ");
  if(c.state.equals(next)){if(!Objects.equals(c.staffNote,note))throw new ApiException(409,"Trạng thái đã lưu với ghi chú khác");return view(c);}
  boolean allowed=next.equals("IN_PROGRESS")&&Set.of("OPEN","RESOLVED").contains(c.state)||next.equals("RESOLVED")&&Set.of("OPEN","IN_PROGRESS").contains(c.state);
  if(!allowed)throw new ApiException(409,"Chuyển trạng thái liên hệ không hợp lệ");
  c.state=next;c.staffNote=note;c.actorId=actor;c.updatedAt=Instant.now();return view(c);
 }
}
