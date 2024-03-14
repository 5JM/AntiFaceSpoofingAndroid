package aero.cubox.communication.utils;

import java.util.HashMap;

import aero.cubox.communication.R;

public enum State {

    NoPermissionToAccess{
        @Override
        public HashMap<String, String> getMsg() {
            HashMap<String, String> result = new HashMap<>();
            result.put("msg", "해당 단말은 미등록 단말이거나,\n유효하지 않은 단말입니다.\n관리자(010-7712-7273) 에게\n문의해 주시기 바랍니다.");
            return result;
        }
    },

    InternalErr{
        @Override
        public HashMap<String, String> getMsg() {
            HashMap<String, String> result = new HashMap<>();
            result.put("code","VR-FAL");
            result.put("guide","실패");
            result.put("msg","작동오류.고객센터로 연락바랍니다.");
            return result;
        }
    }
    ,
    DeviceNoNetwork{
        @Override
        public HashMap<String, String> getMsg() {
            HashMap<String, String> result = new HashMap<>();
            result.put("code","TE-001");
            result.put("guide","실패");
            result.put("msg","네트워크 연결이 원활하지 않습니다. 네트워크 상태를 확인해주세요.");
            return result;
        }
    },
    DeviceNoCamera{
        @Override
        public HashMap<String, String> getMsg() {
            HashMap<String, String> result = new HashMap<>();
            result.put("code","TE-002");
            result.put("guide","실패");
            result.put("msg","카메라가 인식되지 않아 서비스를 이용할 수 없습니다.");
            return result;
        }
    },
    Server401{
        @Override
        public HashMap<String, String> getMsg() {
            HashMap<String, String> result = new HashMap<>();
            result.put("code","HT-401");
            result.put("guide","실패");
            result.put("msg","사용하시는 단말기는 서비스를 이용할 수 없습니다. 고객센터로 연락바랍니다(HTTP 401)");
            return result;
        }
    },
    Server403{
        @Override
        public HashMap<String, String> getMsg() {
            HashMap<String, String> result = new HashMap<>();
            result.put("code","HT-403");
            result.put("guide","실패");
            result.put("msg","사용하시는 단말기는 서비스를 이용할 수 없습니다. 고객센터로 연락바랍니다(HTTP 403)");
            return result;
        }
    },
    Server404{
        @Override
        public HashMap<String, String> getMsg() {
            HashMap<String, String> result = new HashMap<>();
            result.put("code","HT-404");
            result.put("guide","실패");
            result.put("msg","브라우저가 이해할 수 없는 요청을 보냈습니다. 잠시 후 다시 이용해주세요(HTTP 404)");
            return result;
        }
    },
    Server400{
        @Override
        public HashMap<String, String> getMsg() {
            HashMap<String, String> result = new HashMap<>();
            result.put("code","HT-400");
            result.put("guide","실패");
            result.put("msg","브라우저가 이해할 수 없는 요청을 보냈습니다. 잠시 후 다시 이용해주세요(HTTP 400)");
            return result;
        }
    },
    Server500{
        @Override
        public HashMap<String, String> getMsg() {
            HashMap<String, String> result = new HashMap<>();
            result.put("code","HT-500");
            result.put("guide","실패");
            result.put("msg","서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 이용해 주세요(HTTP 500)");
            return result;
        }
    },
    Server503{
        @Override
        public HashMap<String, String> getMsg() {
            HashMap<String, String> result = new HashMap<>();
            result.put("code","HT-503");
            result.put("guide","실패");
            result.put("msg","서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 이용해 주세요(HTTP 503)");
            return result;
        }
    },
    LivenessPassiveModelStart{
        @Override
        public HashMap<String, String> getMsg() {
            HashMap<String, String> result = new HashMap<>();
            result.put("code","VM-001");
            result.put("guide","실패");
            result.put("msg","");
            return result;
        }
    },
    InputParamsErr{
        @Override
        public HashMap<String, String> getMsg() {
            HashMap<String, String> result = new HashMap<>();
            result.put("code","VP-001");
            result.put("guide","실패");
            result.put("msg","요청 파라메터가 부적합 합니다.");
            return result;
        }
    },
    InputNoFace{
        @Override
        public HashMap<String, String> getMsg() {
            HashMap<String, String> result = new HashMap<>();
            result.put("code","VP-002");
            result.put("guide","실패");
            result.put("msg","전달된 사진에서 얼굴정보를 인식할 수 없습니다.");
            return result;
        }
    },
    NoFace{
        @Override
        public HashMap<String, String> getMsg() {
            HashMap<String, String> result = new HashMap<>();
            result.put("code","VC-101");
            result.put("guide","카메라에 얼굴이 확인되지 않습니다.");
            result.put("msg","얼굴을 찾을 수 없어 서비스를 이용할 수 없습니다.");
            return result;
        }
    },

    RealFace{
        @Override
        public HashMap<String, String> getMsg() {
            HashMap<String, String> result = new HashMap<>();
            result.put("code","RealFace");
            result.put("guide","성공");
            result.put("msg","");
            return result;
        }
    },

    MovementLightChange{
        @Override
        public HashMap<String, String> getMsg() {
            HashMap<String, String> result = new HashMap<>();
            result.put("code","VC-102");
            result.put("guide","실패");
            result.put("msg","조도가 좋지 않거나 흔들림이 많습니다.");
            return result;
        }
    },
    NotRealFace{
        @Override
        public HashMap<String, String> getMsg() {
            HashMap<String, String> result = new HashMap<>();
            result.put("code","VC-101");
            result.put("guide","실패");
            result.put("msg","정상적인 얼굴이 아닙니다.");
            return result;
        }
    },
    Success{
        @Override
        public HashMap<String, String> getMsg() {
            HashMap<String, String> result = new HashMap<>();
            result.put("code","VR-SUC");
            result.put("guide","성공");
            result.put("msg","얼굴인증에 성공하였습니다.");
            return result;
        }
    },
    NotMatch{
        @Override
        public HashMap<String, String> getMsg() {
            HashMap<String, String> result = new HashMap<>();
            result.put("code","VR-FAL");
            result.put("guide","실패");
            result.put("msg","얼굴인증에 실패하였습니다. 확인 후 사용 바랍니다.");
            return result;
        }
    };
    public abstract HashMap<String, String> getMsg();
}