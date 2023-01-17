object main extends App {
  /*
    Zadanie:
      pokyny:
        - indexuje sa od 0
        - immutable
        - odovzdava sa struktura ulozena vo "val nazov ..." nie println
        - zadanie needitovat
        - podla vysledku bud zapisem alebo nazapisem novu znamku, predosla pisomka ostava zapisana
      dovysvetlim:
        - maxBy
        - groupBy
        - modulo a delenie
        - s"$a,$b,$c" pattern matching, pozor na poradie pri matchovani
        - println(screen.split("\n").toList)
        - line.replace(" ", "")
      1, v premennej screen su pixel  y v tvare:
          index:red,green,blue,transparency
         kde transparency je optional, tj. su dva druhy pixelov, s priehladnostou a bez nej
         v premennej dimension je hodnost matice, ktoru treba zo struktury vytvorit, teda pre 2 je to matica 2x2 atd
         vysledok tejto ulohy je struktura:
          val pixels: List[Pixel], kde trieda Pixel obsahuje svoju poziciu v matici,
            napr prvok s indexom 3 bude mat poziciu (0, 1) (x,y),
            dalej pixel obsahuje informaciu o farbe a priehladnosti
         priklad (vytlaceny list):
          |TransparentPixel(Pos(0,0),Color(100,56,125),200)|RGBPixel(Pos(1,0),Color(255,0,0))|RGBPixel(Pos(2,0),Color(0,200,135))
          |TransparentPixel(Pos(0,1),Color(220,0,12),100)|RGBPixel(Pos(1,1),Color(45,0,97))|RGBPixel(Pos(2,1),Color(0,0,0))
          |TransparentPixel(Pos(0,2),Color(0,0,0),0)|RGBPixel(Pos(1,2),Color(0,0,0))|RGBPixel(Pos(2,2),Color(255,255,255))
      2, pre kazdy riadok v matici pixelov najdi najcervensi pixel (tj. ten s najvacsou R zlozkou)
         vysledok tejto ulohy je mapa kde kluc je cislo riadku a hodnota najdeny pixel
         priklad:
          Map(
            0 -> RGBPixel(Pos(1,0),Color(255,0,0)),
            1 -> TransparentPixel(Pos(0,1),Color(220,0,12),100),
            2 -> RGBPixel(Pos(2,2),Color(255,255,255))
          )
      3, najdi poziciu najprehladnejsieho pixelu (tj. ma najvacsiu transparency) v celej matici
         vysledok tejto ulohy je dvojica (x, y)
         priklad:
          Pos(0,0)
     */
  val dimension = 3
  val screen =
    """|0:100,125, 56,200
       |1:255,  0,  0
       |2:  0,135,200
       |3:220, 12,  0,100
       |4: 45, 97,  0
       |5:  0,  0,  0
       |6:  0,  0,  0,0
       |7:  0,  0,  0
       |8:255,255,255
       |""".stripMargin
  // koniec zadania


  case class Pos(y:Int,x:Int)


  case class Props(red:Int, green:Int, blue:Int, trans: Option[Int])

  case class Pixel(pos:Pos,props:Props)




  val pixels: List[Pixel] =
    for (line <- screen.split("\n").toList.map(_.replace(" ",""))) yield {
      line.trim match{
//        case s"|$id:$red,$green,$blue,$trans" =>{println(id)
//          Pixel(Pos(id.toInt, 0), Props(red.toInt, green.toInt, blue.toInt, trans.toIntOption))}

        case s"$id:$red,$green,$blue,$trans" =>{println(id)
          Pixel(Pos(id.toInt / 3 , id.toInt % 3), Props(red.toInt, green.toInt, blue.toInt, trans.toIntOption))}

        case s"$id:$red,$green,$blue" => {println(id)
          Pixel(Pos(id.toInt / 3 , id.toInt % 3), Props(red.toInt, green.toInt, blue.toInt, None))}

      }
    }
  println(pixels)


  val red : Map[Int , Pixel] =  pixels.groupBy(_.pos.y).map{
    case (riadok,pixel) => (riadok,pixel.maxBy(_.props.red))
  }
  println("max reds --> " + red)
  val trans : Map[Int,Int] = pixels.groupBy(_.pos.y).map{
    case (riadok,pixel) => (riadok,(pixel.maxBy(_.props.trans)).pos.x)
  }
  println("max trans --> " + trans)

}
