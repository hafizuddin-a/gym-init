// make_logo.swift — draws assets/logo.png, the README logo: the app icon's glyph (a green ">" prompt and an
// amber "_" cursor, same geometry as app/src/main/res/drawable/ic_launcher_foreground.xml) on the tokyo night
// background, in a rounded square.
//   swiftc -O tools/make_logo.swift -o /tmp/make_logo && /tmp/make_logo assets/logo.png
// (tools/make_logo.sh wraps this.)
import AppKit
import ImageIO
import UniformTypeIdentifiers

let outPath = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "assets/logo.png"
let pixels = 512

func rgb(_ hex: UInt32, _ alpha: CGFloat = 1) -> CGColor {
    CGColor(red: CGFloat((hex >> 16) & 0xFF) / 255, green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255, alpha: alpha)
}

/// A 824 pt body centred on a 1024 pt canvas, corners closer to a superellipse than a circle.
func squircle(half: CGFloat, center: CGPoint, exponent n: Double = 5) -> CGPath {
    let path = CGMutablePath()
    let steps = 720
    for i in 0...steps {
        let t = Double(i) / Double(steps) * 2 * .pi
        let c = cos(t), s = sin(t)
        let x = Double(half) * (c < 0 ? -1 : 1) * pow(abs(c), 2 / n)
        let y = Double(half) * (s < 0 ? -1 : 1) * pow(abs(s), 2 / n)
        let p = CGPoint(x: center.x + CGFloat(x), y: center.y + CGFloat(y))
        if i == 0 { path.move(to: p) } else { path.addLine(to: p) }
    }
    path.closeSubpath()
    return path
}

let ctx = CGContext(data: nil, width: pixels, height: pixels, bitsPerComponent: 8, bytesPerRow: 0,
                    space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
let k = CGFloat(pixels) / 1024
ctx.scaleBy(x: k, y: k)
let centre = CGPoint(x: 512, y: 512)
let body = squircle(half: 412, center: centre)

// soft drop shadow under the body
ctx.saveGState()
ctx.setShadow(offset: CGSize(width: 0, height: -14), blur: 28, color: CGColor(gray: 0, alpha: 0.45))
ctx.addPath(body)
ctx.setFillColor(rgb(0x1A1B26))
ctx.fillPath()
ctx.restoreGState()

// the app's background colour, with a faint lift at the top
ctx.saveGState()
ctx.addPath(body)
ctx.clip()
let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(),
                          colors: [rgb(0x262838), rgb(0x1A1B26)] as CFArray, locations: [0, 0.75])!
ctx.drawLinearGradient(gradient, start: CGPoint(x: 512, y: 924), end: CGPoint(x: 512, y: 100), options: [])
ctx.restoreGState()

// hairline edge
ctx.addPath(squircle(half: 411, center: centre))
ctx.setStrokeColor(CGColor(gray: 1, alpha: 0.10))
ctx.setLineWidth(2)
ctx.strokePath()

// the glyph, from the launcher icon's 108-unit viewport (centred on 57,54), scaled to ~46% of the body width
let s: CGFloat = (824 * 0.46) / 44
func pt(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: 512 + (x - 57) * s, y: 512 - (y - 54) * s) }
ctx.setLineCap(.round)
ctx.setLineJoin(.round)
ctx.setLineWidth(6 * s)

ctx.setStrokeColor(rgb(0x9ECE6A))
ctx.move(to: pt(38, 38)); ctx.addLine(to: pt(56, 54)); ctx.addLine(to: pt(38, 70))
ctx.strokePath()

ctx.setStrokeColor(rgb(0xE0AF68))
ctx.move(to: pt(62, 72)); ctx.addLine(to: pt(76, 72))
ctx.strokePath()

let url = URL(fileURLWithPath: outPath) as CFURL
let dst = CGImageDestinationCreateWithURL(url, UTType.png.identifier as CFString, 1, nil)!
CGImageDestinationAddImage(dst, ctx.makeImage()!, nil)
CGImageDestinationFinalize(dst)
print("Wrote \(outPath)")
