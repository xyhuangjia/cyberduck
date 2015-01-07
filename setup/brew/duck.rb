class Duck < Formula
  homepage "https://duck.sh/"
  url "${SOURCE}"
  head "https://svn.cyberduck.io/trunk/"

  depends_on :java => :build
  depends_on :xcode => :build
  depends_on "ant" => :build
  depends_on "openssl"

  def install
    system "ant", "-Dbuild.compile.target=1.7", "-Drevision=${REVISION}", "cli"
    system "install_name_tool", "-change", "/usr/lib/libcrypto.0.9.8.dylib", "/usr/local/opt/openssl/lib/libcrypto.dylib", "build/libPrime.dylib"
    system "install_name_tool", "-change", "/usr/lib/libcrypto.0.9.8.dylib", "/usr/local/opt/openssl/lib/libcrypto.dylib", "build/librococoa.dylib"
    libexec.install Dir["build/duck.bundle/*"]
    bin.install_symlink "#{libexec}/Contents/MacOS/duck" => "duck"
  end

  test do
    system "#{bin}/duck", "-version"
  end
end
