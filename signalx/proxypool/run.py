from proxypool.scheduler import Scheduler
from install_missing_packages import install_missing_packages
import argparse

parser = argparse.ArgumentParser(description='ProxyPool')
parser.add_argument('--processor', type=str, help='processor to run')
args = parser.parse_args()

@install_missing_packages()
def main():
    # if processor set, just run it
    if args.processor:
        getattr(Scheduler(), f'run_{args.processor}')()
    else:
        Scheduler().run()


if __name__ == '__main__':
    main()
