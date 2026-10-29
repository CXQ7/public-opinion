from enum import Enum
from selenium import webdriver
from selenium.webdriver.edge.options import Options as EdgeOptions
from selenium.webdriver.chrome.options import Options as ChromeOptions

class WebDriverTypeEnum(Enum):
    CHROME = "chrome"
    FIREFOX = "firefox"
    EDGE = "edge"

class SeleniumConfig:
    def __init__(self, driver_type: WebDriverTypeEnum = WebDriverTypeEnum.CHROME):
        self.driver_type = driver_type

    def _get_options(self):
        if self.driver_type == WebDriverTypeEnum.EDGE:
            options = EdgeOptions()
        else:
            options = ChromeOptions()

        # options.add_argument('--headless')  #无界面运行
        options.add_argument('headers')  # 添加请求头
        options.add_argument('-ignore-certificate-errors')
        options.add_argument('-ignore -ssl-errors')
        options.add_experimental_option('excludeSwitches', ['enable-automation'])  #设置开发者模式启动
        options.add_argument("disable-blink-features=AutomationControlled")

        # 似乎不用
        # if self.driver_type == WebDriverTypeEnum.CHROME:
        #     return ChromeOptions()
        # elif self.driver_type == WebDriverTypeEnum.FIREFOX:
        #     return FirefoxOptions()
        # elif self.driver_type == WebDriverTypeEnum.EDGE:
        #     return EdgeOptions()
        # else:
        #     raise ValueError(f"不支持的浏览器类型: {self.driver_type}")

        return options

    def get_browser(self):
        options = self._get_options()
        if self.driver_type == WebDriverTypeEnum.CHROME:
            return webdriver.Chrome(options=options)
        elif self.driver_type == WebDriverTypeEnum.FIREFOX:
            return webdriver.Firefox(options=options)
        elif self.driver_type == WebDriverTypeEnum.EDGE:
            return webdriver.Edge(options=options)
        else:
            raise ValueError(f"不支持的浏览器类型: {self.driver_type}")

        # 隐式等待时间, 通常不需要配置, 而是程序内部手动控制等待时间
        # self.browser.implicitly_wait(10)


selenium_config = SeleniumConfig(WebDriverTypeEnum.CHROME)
