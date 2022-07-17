package com.encore.byebuying.domain.user.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.encore.byebuying.config.properties.AppProperties;
import com.encore.byebuying.domain.user.User;
import com.encore.byebuying.domain.user.UserRefreshToken;
import com.encore.byebuying.domain.user.dto.UserDTO;
import com.encore.byebuying.domain.user.dto.UserInfoDTO;
import com.encore.byebuying.domain.user.repository.LocationRepository;
import com.encore.byebuying.domain.user.repository.UserRefreshTokenRepository;
import com.encore.byebuying.domain.platfrom2server.service.WebClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.encore.byebuying.domain.user.Location;
import com.encore.byebuying.domain.user.service.UserService;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final LocationRepository locationRepository;
    private final WebClientService webClientService;
    private final PasswordEncoder passwordEncoder;
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final AppProperties appProperties;

    @PostMapping
    public ResponseEntity<?> saveUser(@RequestBody UserDTO userDTO) {
        String username = userService.saveUser(userDTO);
//        webClientService.newUser(newUser.getUsername());
        return new ResponseEntity<>(username, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<?> getUser(@RequestParam String username) {
        UserInfoDTO user = userService.getUserInfo(username);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping("/sy/user") // 관리자 유저들 확인 todo: 관리자 UserDTO 나중에 수정, 유저 정보에 들어갈 것들 생각 필요함
    public ResponseEntity<Page<User>> getUsers(
            @RequestParam(required = false, defaultValue="1",value="page") int page) {
        Pageable pageable = PageRequest.of(page-1, 20, Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok().body(userService.getUsers(pageable));
    }

    // todo: 수정 or 삭제 시 비밀번호 확인할 것인지 확인 필요
//    @PostMapping("/user/getUser") // 회원 확인용 - 수정 or 삭제
//    public ResponseEntity<User> getUser(@RequestBody User userinfo) {
//        User user = userService.getUser(userinfo.getUsername());
//        System.out.println(userinfo.getUsername()+" "+userinfo.getPassword());
//        if (passwordEncoder.matches(userinfo.getPassword(), user.getPassword())){
//            return ResponseEntity.ok().body(user);
//        }
//        return ResponseEntity.badRequest().body(null);
//    }

    @GetMapping("/location") // 회원 배송지
    public ResponseEntity<?> getUserLocation(@RequestParam String username) {
        Collection<Location> locations = userService.getLocation(username);
        return new ResponseEntity<>(locations, HttpStatus.OK);
    }

    @GetMapping("/check") // 아이디 중복 검사 확인
    public ResponseEntity<?> checkUser(
            @RequestParam(defaultValue = "", value = "username") String username) {
        String checkValue = userService.checkUser(username);
        return new ResponseEntity<>(checkValue, HttpStatus.OK);
    }

    @DeleteMapping // 토큰 필요, 삭제 전 /api/user/getUser 에서 토큰 및 비밀번호 확인
    public ResponseEntity<?> deleteUser(
    		@RequestParam(defaultValue = "", value="username") String username) {
        userService.deleteUser(username);
        return new ResponseEntity<>("SUCCESS", HttpStatus.OK);
    }

    // todo: 수정 필요
    @PutMapping // 토큰 필요
    public ResponseEntity<?> updateUser(@RequestBody UserDTO userForm) {
        User user = userService.getUser(userForm.getUsername());
        if (user == null){
            return new ResponseEntity<>("FAIL", HttpStatus.OK);
        }
    //        if (userForm.getPassword() != null && !userForm.getPassword().equals("")) // 비밀번호도 수정될 때
    //            user.setPassword(userForm.getPassword());
    //        user.setEmail(userForm.getEmail());
    //        user.setDefaultLocationIdx(userForm.getDefaultLocationIdx());

        // 현재주소 갈아엎고 새주소 넣기
        List<Location> list = (List<Location>) user.getLocations();
        Long[] idList = new Long[list.size()];
        for(int i=0;i<list.size();i++)
          idList[i]=list.get(i).getId();

        user.getLocations().clear();
        for(Long id : idList) {
          locationRepository.deleteById(id);
        }

        user.getLocations().addAll(userForm.getLocations());
    //        userService.saveUser(user);
        return new ResponseEntity<>("SUCCESS", HttpStatus.OK);
    }



//    @PostMapping("/user/update/admin") // 관리자가 유저 정보 수정
//    public ResponseEntity<?> adminUpdateUser(@RequestBody UserForm userForm){
////        {
////            "username" : "qwer1234",
////                "password" : "12341234",
////                "email" : "JHJKINGAKKKKKK@ByeBuying.com",
////                "locations" : [{"location":"외곽"},{"location":"외곽2"}]
////        }
//        System.out.println("userForm = "+userForm);
//        User user = userService.getUser(userForm.getUsername());
//        System.out.println("user = " + user);
//        if (user == null){
//            return new ResponseEntity<>("FAIL", HttpStatus.OK);
//        }
//        if (userForm.getPassword() != null && !userForm.getPassword().equals("")) // 비밀번호도 수정될 때
//            user.setPassword(userForm.getPassword());
//        user.setEmail(userForm.getEmail());
//        user.setDefaultLocationIdx(userForm.getDefaultLocationIdx());
//
//        // 현재주소 갈아엎고 새주소 넣기
//        List<Location> list = (List<Location>) user.getLocations();
//        System.out.println("list = " + list);
//        Long[] idList = new Long[list.size()];
//        System.out.println("idListBefore = " + idList);
//        // 위치 인덱스 번호를 뺴옴
//        for(int i=0;i<list.size();i++){
//            idList[i]=list.get(i).getId();
//        }
//        user.getLocations().clear();
//        if (user.getLocations() != null){
//            for(Long id : idList) {
//                locationRepo.deleteById(id);
//            }
//        }
//        System.out.println("userForm.getLocations() = " + userForm.getLocations());
//        user.getLocations().addAll(userForm.getLocations());
//        userService.saveUser(user);
//        return new ResponseEntity<>("SUCCESS", HttpStatus.OK);
//    }

    @GetMapping("/token/refresh")
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION);
        if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            try {
                // request의 header에 "Bearer token~~~~" 형식으로 전달되기 때문에 "Bearer " 문자 제거
                String refresh_token = authorizationHeader.substring("Bearer ".length());
                // HMAC256 활용
                Algorithm algorithm = Algorithm.HMAC256(appProperties.getAuth().getTokenSecret().getBytes());
                // verifier에 alhorithm을 적용하여 refreshToken에 대한 유효성 확인
                JWTVerifier verifier = JWT.require(algorithm).build();
                DecodedJWT decodedJWT = verifier.verify(refresh_token);

                String username = decodedJWT.getSubject();
                User user = userService.getUser(username);

                // DB에 저장된 refresh token과 동일한지 확인
                UserRefreshToken userRefreshToken = userRefreshTokenRepository.findByUsername(username);
                if (userRefreshToken == null || !userRefreshToken.getRefreshToken().equals(refresh_token)) {
                    throw new RuntimeException("Refresh token is missing or miss match");
                }

                // 확인되었다면 새로운 access token 발급한 후 반환
                String access_token = JWT.create()
                        .withSubject(user.getUsername())
                        .withExpiresAt(new Date(System.currentTimeMillis() + appProperties.getAuth().getAccesstokenExpiration())) // 10분
                        .withIssuer(request.getRequestURL().toString())
                        .withClaim("role", user.getRoleType().getCode())
                        .sign(algorithm); // 토큰 서명

                Map<String, String> tokens = new HashMap<>();
                tokens.put("access_token", access_token);
                tokens.put("refresh_token", refresh_token);

                response.setContentType(APPLICATION_JSON_VALUE);

                new ObjectMapper().writeValue(response.getOutputStream(), tokens);
            }catch (Exception exception) {
                response.setHeader("error", exception.getMessage());
                response.setStatus(FORBIDDEN.value());

                Map<String, String> error = new HashMap<>();
                error.put("error_message", exception.getMessage());
                response.setContentType(MimeTypeUtils.APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), error);
            }
        } else {
            throw new RuntimeException("Refresh token is missing");
        }
    }
}
