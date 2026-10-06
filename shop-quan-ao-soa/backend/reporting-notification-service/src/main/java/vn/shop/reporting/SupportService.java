package vn.shop.reporting;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.shop.common.*;
import vn.shop.reporting.SupportDtos.*;

@Service
public class SupportService {
 private final SupportTicketRepository tickets;private final SupportMessageRepository messages;
 private final SupportHistoryRepository history;private final SupportGuardRepository guard;
 public SupportService(SupportTicketRepository tickets,SupportMessageRepository messages,SupportHistoryRepository history,SupportGuardRepository guard){this.tickets=tickets;this.messages=messages;this.history=history;this.guard=guard;}
 private String hash(String value){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));return HexFormat.of().formatHex(bytes);}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private void key(String key){if(key==null||!key.matches("[A-Za-z0-9_-]{8,100}"))throw new ApiException(400,"Idempotency-Key không hợp lệ");}
 private SupportTicket require(long id,long user,boolean manager){var t=tickets.findById(id).orElseThrow(ApiException::missing);if(!manager&&t.userId!=user)throw ApiException.missing();return t;}
 private TicketView view(SupportTicket t){return new TicketView(t.id,t.userId,t.subject,t.state,t.createdAt,t.updatedAt);}
 private void status(SupportTicket t,String state,long actor){if(t.state.equals(state))return;t.state=state;t.updatedAt=Instant.now();SupportStatusHistory h=new SupportStatusHistory();h.ticketId=t.id;h.state=state;h.actorId=actor;history.save(h);}
 @Transactional public Detail create(long user,String requestKey,Create in){
  key(requestKey);String subject=in.subject().trim(),body=in.message().trim(),fingerprint=hash(subject+"\u0000"+body);guard.lock();
  var previous=tickets.findByUserIdAndRequestKey(user,requestKey);if(previous.isPresent()){if(!previous.get().requestHash.equals(fingerprint))throw new ApiException(409,"Mã yêu cầu đã dùng với nội dung khác");return detail(previous.get().id,user,false);}
  SupportTicket t=new SupportTicket();t.userId=user;t.requestKey=requestKey;t.requestHash=fingerprint;t.subject=subject;t=tickets.save(t);
  SupportMessage m=new SupportMessage();m.ticketId=t.id;m.authorId=user;m.authorRole="CUSTOMER";m.requestKey=requestKey;m.requestHash=hash(body);m.messageText=body;messages.save(m);
  SupportStatusHistory h=new SupportStatusHistory();h.ticketId=t.id;h.state="OPEN";h.actorId=user;history.save(h);return detail(t.id,user,false);
 }
 @Transactional(readOnly=true) public PageResult<TicketView> list(Long owner,String state,String q,int page){
  if(state!=null&&!Set.of("OPEN","IN_PROGRESS","WAITING_CUSTOMER","RESOLVED","CLOSED").contains(state))throw new ApiException(400,"Trạng thái hỗ trợ không hợp lệ");
  var p=tickets.search(owner,state,q==null||q.isBlank()?null:q.trim(),PageRequest.of(page,20,Sort.by("updatedAt").descending().and(Sort.by("id").descending())));
  return new PageResult<>(p.map(this::view).getContent(),p.getTotalElements(),p.getTotalPages(),page,20,p.isLast());
 }
 @Transactional(readOnly=true) public Detail detail(long id,long user,boolean manager){var t=require(id,user,manager);return new Detail(view(t),messages.findByTicketIdOrderByCreatedAtAscIdAsc(id).stream().map(m->new MessageView(m.id,m.authorId,m.authorRole,m.messageText,m.createdAt)).toList(),history.findByTicketIdOrderByCreatedAtAscIdAsc(id).stream().map(h->new StatusView(h.state,h.actorId,h.createdAt)).toList());}
 @Transactional public Detail reply(long id,long user,String role,String requestKey,Reply in){
  key(requestKey);boolean manager=role.equals("ADMIN")||role.equals("STAFF");guard.lock();var t=require(id,user,manager);String body=in.message().trim(),fingerprint=hash(body);
  var previous=messages.findByTicketIdAndRequestKey(id,requestKey);if(previous.isPresent()){if(!previous.get().requestHash.equals(fingerprint)||previous.get().authorId!=user)throw new ApiException(409,"Mã yêu cầu đã dùng với nội dung khác");return detail(id,user,manager);}
  if(t.state.equals("CLOSED")||(manager&&t.state.equals("RESOLVED")))throw new ApiException(409,"Yêu cầu đã đóng hoặc đã xử lý xong");
  SupportMessage m=new SupportMessage();m.ticketId=id;m.authorId=user;m.authorRole=role;m.requestKey=requestKey;m.requestHash=fingerprint;m.messageText=body;messages.save(m);
  status(t,manager?"WAITING_CUSTOMER":"IN_PROGRESS",user);t.updatedAt=Instant.now();return detail(id,user,manager);
 }
 @Transactional public Detail change(long id,long user,boolean manager,String target){
  guard.lock();var t=require(id,user,manager);if(t.state.equals(target))return detail(id,user,manager);
  if(!manager&&!(t.state.equals("RESOLVED")&&target.equals("CLOSED")))throw new ApiException(403,"Khách chỉ được đóng yêu cầu đã xử lý");
  if(manager){boolean allowed=switch(target){case "IN_PROGRESS"->Set.of("OPEN","WAITING_CUSTOMER","RESOLVED").contains(t.state);case "RESOLVED"->Set.of("OPEN","IN_PROGRESS","WAITING_CUSTOMER").contains(t.state);case "CLOSED"->t.state.equals("RESOLVED");default->false;};if(!allowed)throw new ApiException(409,"Chuyển trạng thái hỗ trợ không hợp lệ");}
  status(t,target,user);return detail(id,user,manager);
 }
}
