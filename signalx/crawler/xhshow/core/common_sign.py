import json
import os
import sys
from typing import Any

# 获取当前文件的绝对路径
current_file = os.path.abspath(__file__)
# 向上跳三级寻找项目根目录: core -> xhshow -> crawler
BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(current_file)))

if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)


try:
    from xhshow.config import CryptoConfig
    from xhshow.core.crc32_encrypt import CRC32
    from xhshow.utils.encoder import Base64Encoder
except ModuleNotFoundError as e:
    # 兜底：如果直接在 xhshow 目录下运行（此时可能找不到包名 xhshow）
    if e.name == "xhshow":
        from config import CryptoConfig
        from core.crc32_encrypt import CRC32
        from utils.encoder import Base64Encoder
    else:
        raise

try:
    from xhshow.generators.fingerprint import FingerprintGenerator
except ModuleNotFoundError as e:
    # 指纹生成依赖 Crypto（来自 pycryptodome）。缺依赖时直接提示，不要走“运行目录兜底”。
    if e.name == "Crypto":
        raise ModuleNotFoundError(
            "缺少依赖模块 'Crypto'：请安装 'pycryptodome'（pip install pycryptodome）。"
        ) from e
    if e.name == "xhshow":
        from generators.fingerprint import FingerprintGenerator
    else:
        raise
except ImportError:
    # 兼容直接在 xhshow 目录下运行（相对导入）
    from generators.fingerprint import FingerprintGenerator

__all__ = ["XsCommonSigner"]


class XsCommonSigner:
    """Generate x-s-common signatures"""

    def __init__(self, config: CryptoConfig | None = None):
        self.config = config or CryptoConfig()
        self._fp_generator = FingerprintGenerator(self.config)
        self._encoder = Base64Encoder(self.config)

    def sign(self, cookie_dict: dict[str, Any]) -> str:
        """
        Generate x-s-common signature

        Args:
            cookie_dict: Cookie dictionary (must be dict, not string)

        Returns:
            x-s-common signature string

        Raises:
            KeyError: If 'a1' cookie is missing
        """
        a1_value = cookie_dict["a1"]
        fingerprint = self._fp_generator.generate(cookies=cookie_dict, user_agent=self.config.PUBLIC_USERAGENT)
        b1 = self._fp_generator.generate_b1(fingerprint)

        x9 = CRC32.crc32_js_int(b1)

        sign_struct = dict(self.config.SIGNATURE_XSCOMMON_TEMPLATE)
        sign_struct["x5"] = a1_value
        sign_struct["x8"] = b1
        sign_struct["x9"] = x9

        sign_json = json.dumps(sign_struct, separators=(",", ":"), ensure_ascii=False)
        xs_common = self._encoder.encode(sign_json)

        return xs_common
